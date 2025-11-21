package cg.xis.eradio.service;

import cg.xis.eradio.domain.entity.Favorite;
import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.domain.repository.FavoriteRepository;
import cg.xis.eradio.domain.repository.RadioStationRepository;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.dto.response.RecommendationsResponse;
import cg.xis.eradio.infrastructure.client.RadioBrowserApiClient;
import cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto;
import cg.xis.eradio.infrastructure.mapper.RadioStationMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationService {

    private final ChatModel chatModel;
    private final FavoriteRepository favoriteRepository;
    private final RadioStationRepository radioStationRepository;
    private final RadioBrowserApiClient radioBrowserApiClient;
    private final RadioStationMapper radioStationMapper;
    private final ObjectMapper objectMapper;

    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int CANDIDATE_POOL_SIZE = 100;

    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", keyGenerator = "recommendationsKeyGenerator")
    public RecommendationsResponse getRecommendations(User user, int limit) {
        log.debug("Generating fresh recommendations for user: {}", user.getId());
        // Get user's favorite stations
        List<Favorite> favorites = favoriteRepository.findByUser(user,
                PageRequest.of(0, 50)).getContent();

        if (favorites.isEmpty()) {
            return RecommendationsResponse.builder()
                    .recommendations(Collections.emptyList())
                    .reason("Add some stations to your favorites to get personalized recommendations!")
                    .build();
        }

        // Extract favorite station metadata
        List<RadioStation> favoriteStations = favorites.stream()
                .map(Favorite::getRadioStation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (favoriteStations.isEmpty()) {
            return RecommendationsResponse.builder()
                    .recommendations(Collections.emptyList())
                    .reason("Unable to load your favorite stations. Please try again later.")
                    .build();
        }

        // Build user profile from favorites
        String userProfile = buildUserProfile(favoriteStations);

        // Get candidate stations from RadioBrowser API
        // Search for stations similar to user's favorites
        List<RadioBrowserStationDto> candidateStations = getCandidateStations(favoriteStations);

        if (candidateStations.isEmpty()) {
            return RecommendationsResponse.builder()
                    .recommendations(Collections.emptyList())
                    .reason("No similar stations found. Try adding more diverse stations to your favorites.")
                    .build();
        }

        // Filter out stations already in favorites
        Set<String> favoriteUuids = favoriteStations.stream()
                .map(RadioStation::getStationUuid)
                .collect(Collectors.toSet());

        List<RadioBrowserStationDto> filteredCandidates = candidateStations.stream()
                .filter(station -> !favoriteUuids.contains(station.getStationUuid()))
                .limit(CANDIDATE_POOL_SIZE)
                .collect(Collectors.toList());

        // Use AI to select and rank recommendations
        List<String> recommendedUuids = getAiRecommendations(userProfile, filteredCandidates, limit);

        // Fetch recommended stations
        List<RadioStationResponse> recommendations = recommendedUuids.stream()
                .map(uuid -> {
                    try {
                        RadioBrowserStationDto dto = radioBrowserApiClient.getStationByUuid(uuid);
                        if (dto != null) {
                            // Check if station is in DB and if it's a favorite
                            RadioStation dbStation = radioStationRepository.findByStationUuid(uuid).orElse(null);
                            boolean isFavorite = false;
                            if (dbStation != null) {
                                isFavorite = favoriteRepository.existsByUserAndRadioStation(user, dbStation);
                            }

                            return radioStationMapper.toResponse(dto, isFavorite);
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Failed to fetch station with UUID: {}", uuid, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Generate explanation
        String reason = generateRecommendationReason(favoriteStations, recommendations);

        return RecommendationsResponse.builder()
                .recommendations(recommendations)
                .reason(reason)
                .build();
    }

    private String buildUserProfile(List<RadioStation> favoriteStations) {
        StringBuilder profile = new StringBuilder("User's favorite radio stations:\n\n");

        for (RadioStation station : favoriteStations) {
            profile.append(String.format("- Name: %s\n", station.getName()));
            if (station.getTags() != null && !station.getTags().isEmpty()) {
                profile.append(String.format("  Tags: %s\n", station.getTags()));
            }
            if (station.getCountry() != null) {
                profile.append(String.format("  Country: %s\n", station.getCountry()));
            }
            if (station.getLanguage() != null) {
                profile.append(String.format("  Language: %s\n", station.getLanguage()));
            }
            profile.append("\n");
        }

        // Extract common patterns
        Map<String, Long> tagFrequency = favoriteStations.stream()
                .filter(s -> s.getTags() != null && !s.getTags().isEmpty())
                .flatMap(s -> Arrays.stream(s.getTags().split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.groupingBy(tag -> tag.toLowerCase(), Collectors.counting()));

        if (!tagFrequency.isEmpty()) {
            profile.append("Common themes in favorites:\n");
            tagFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> profile.append(String.format("- %s (appears %d times)\n",
                            entry.getKey(), entry.getValue())));
        }

        return profile.toString();
    }

    private List<RadioBrowserStationDto> getCandidateStations(List<RadioStation> favoriteStations) {
        Set<RadioBrowserStationDto> candidates = new HashSet<>();

        // Get stations by tags from favorites
        favoriteStations.stream()
                .filter(s -> s.getTags() != null && !s.getTags().isEmpty())
                .flatMap(s -> Arrays.stream(s.getTags().split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .limit(5)
                .forEach(tag -> {
                    try {
                        List<RadioBrowserStationDto> stations = radioBrowserApiClient.searchStations(
                                null, null, null, tag, 20);
                        if (stations != null) {
                            candidates.addAll(stations);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to search stations by tag: {}", tag, e);
                    }
                });

        // Get stations by country from favorites
        favoriteStations.stream()
                .filter(s -> s.getCountry() != null && !s.getCountry().isEmpty())
                .map(RadioStation::getCountry)
                .distinct()
                .limit(3)
                .forEach(country -> {
                    try {
                        List<RadioBrowserStationDto> stations = radioBrowserApiClient.searchStations(
                                null, country, null, null, 20);
                        if (stations != null) {
                            candidates.addAll(stations);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to search stations by country: {}", country, e);
                    }
                });

        return new ArrayList<>(candidates);
    }

    private List<String> getAiRecommendations(String userProfile,
            List<RadioBrowserStationDto> candidates,
            int limit) {
        try {
            // Build candidate stations JSON
            String candidatesJson = buildCandidatesJson(candidates);

            // Create prompt for AI
            PromptTemplate promptTemplate = new PromptTemplate(
                    """
                            You are a radio station recommendation expert. Based on a user's favorite stations, recommend new stations they might enjoy.

                            User's favorite stations profile:
                            {userProfile}

                            Candidate stations to choose from:
                            {candidates}

                            Analyze the user's preferences and select the top {limit} stations that best match their taste.
                            Consider:
                            - Similar genres/tags
                            - Similar countries/languages
                            - Music style compatibility
                            - Diversity (don't recommend only identical stations)

                            Return ONLY a JSON array of station UUIDs in this exact format:
                            ["uuid1", "uuid2", "uuid3", ...]

                            Do not include any explanation, only the JSON array.
                            """);

            Map<String, Object> variables = new HashMap<>();
            variables.put("userProfile", userProfile);
            variables.put("candidates", candidatesJson);
            variables.put("limit", limit);

            Prompt prompt = promptTemplate.create(variables);
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            // Parse JSON response
            return parseUuidArray(response);

        } catch (Exception e) {
            log.error("Failed to get AI recommendations", e);
            // Fallback: return top candidates by votes
            return candidates.stream()
                    .sorted(Comparator.comparing(RadioBrowserStationDto::getVotes,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .map(RadioBrowserStationDto::getStationUuid)
                    .collect(Collectors.toList());
        }
    }

    private String buildCandidatesJson(List<RadioBrowserStationDto> candidates) {
        try {
            List<Map<String, Object>> candidateList = candidates.stream()
                    .map(dto -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("stationUuid", dto.getStationUuid());
                        map.put("name", dto.getName());
                        map.put("tags", dto.getTags());
                        map.put("country", dto.getCountry());
                        map.put("language", dto.getLanguage());
                        map.put("votes", dto.getVotes());
                        return map;
                    })
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(candidateList);
        } catch (JsonProcessingException e) {
            log.error("Failed to build candidates JSON", e);
            return "[]";
        }
    }

    private List<String> parseUuidArray(String jsonResponse) {
        try {
            // Clean response - remove markdown code blocks if present
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            String[] uuids = objectMapper.readValue(cleaned, String[].class);
            return Arrays.asList(uuids);
        } catch (Exception e) {
            log.error("Failed to parse UUID array from AI response: {}", jsonResponse, e);
            return Collections.emptyList();
        }
    }

    private String generateRecommendationReason(List<RadioStation> favorites,
            List<RadioStationResponse> recommendations) {
        try {
            String favoritesSummary = favorites.stream()
                    .map(RadioStation::getName)
                    .limit(3)
                    .collect(Collectors.joining(", "));

            PromptTemplate promptTemplate = new PromptTemplate(
                    """
                            Generate a brief, friendly explanation (1-2 sentences) for why these radio stations were recommended.

                            User's favorite stations include: {favorites}

                            Number of recommendations: {count}

                            Keep it concise and natural. Focus on the themes, genres, or styles that connect the favorites to the recommendations.
                            """);

            Map<String, Object> variables = new HashMap<>();
            variables.put("favorites", favoritesSummary);
            variables.put("count", recommendations.size());

            Prompt prompt = promptTemplate.create(variables);
            return chatModel.call(prompt).getResult().getOutput().getText().trim();
        } catch (Exception e) {
            log.error("Failed to generate recommendation reason", e);
            return "Based on your favorite stations, we think you'll enjoy these recommendations!";
        }
    }
}
