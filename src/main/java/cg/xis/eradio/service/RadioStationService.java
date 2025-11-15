package cg.xis.eradio.service;

import cg.xis.eradio.domain.entity.Favorite;
import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.domain.repository.FavoriteRepository;
import cg.xis.eradio.domain.repository.RadioStationRepository;
import cg.xis.eradio.dto.request.StationSearchRequest;
import cg.xis.eradio.dto.response.PageResponse;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.infrastructure.client.RadioBrowserApiClient;
import cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto;
import cg.xis.eradio.infrastructure.mapper.RadioStationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RadioStationService {

    private static final int MAX_BATCH_SIZE = 500;

    private final RadioStationRepository radioStationRepository;
    private final FavoriteRepository favoriteRepository;
    private final RadioBrowserApiClient radioBrowserApiClient;
    private final RadioStationMapper radioStationMapper;

    /**
     * Finds radio stations by their UUIDs with automatic batching to prevent large
     * IN queries.
     * <p>
     * This method splits large collections into batches of at most MAX_BATCH_SIZE
     * and aggregates
     * the results. This prevents database performance issues from very large IN
     * queries.
     * </p>
     *
     * @param stationUuids collection of station UUIDs to search for
     * @return list of matching RadioStation entities
     * @throws IllegalArgumentException if stationUuids is null
     */
    List<RadioStation> findByStationUuidInBatched(Collection<String> stationUuids) {
        if (stationUuids == null) {
            throw new IllegalArgumentException("Station UUIDs collection cannot be null");
        }

        if (stationUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uuidList = new ArrayList<>(stationUuids);
        List<RadioStation> results = new ArrayList<>();

        // Split into batches and process each batch
        for (int i = 0; i < uuidList.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, uuidList.size());
            List<String> batch = uuidList.subList(i, end);
            List<RadioStation> batchResults = radioStationRepository.findByStationUuidIn(batch);
            results.addAll(batchResults);
        }

        return results;
    }

    /**
     * Resolves which station UUIDs from the given set are favorites for the user.
     * <p>
     * This method fetches RadioStation entities by UUID, maps them to IDs, queries
     * favorites, and returns the UUIDs of stations that are favorited by the user.
     * </p>
     * <p>
     * Note: This method filters out favorites with null radioStation references.
     * Consider adding a database constraint or cleanup migration to prevent
     * favorites with null radioStation references for data integrity.
     * </p>
     *
     * @param stationUuids set of station UUIDs to check
     * @param user         the user to check favorites for
     * @return set of station UUIDs that are favorites for the user
     */
    private Set<String> resolveFavoriteUuids(Set<String> stationUuids, User user) {
        if (stationUuids == null || stationUuids.isEmpty()) {
            return Set.of();
        }

        List<RadioStation> stations = findByStationUuidInBatched(stationUuids);
        if (stations.isEmpty()) {
            return Set.of();
        }

        Set<Long> ids = stations.stream()
                .map(RadioStation::getId)
                .collect(Collectors.toSet());

        return favoriteRepository.findByUserAndRadioStationIdIn(user, ids)
                .stream()
                .map(Favorite::getRadioStation)
                .filter(Objects::nonNull)
                .map(RadioStation::getStationUuid)
                .collect(Collectors.toSet());
    }

    /**
     * Searches for radio stations using the RadioBrowser API.
     *
     * Note: The RadioBrowser API only supports a limit parameter (no offset/page
     * support).
     * This method fetches up to 1000 results and performs in-memory pagination.
     * The totalElements and totalPages reflect the actual number of results
     * returned by the API,
     * not the total available results. Pagination is limited to the first 1000
     * results.
     */
    @Transactional(readOnly = true)
    public PageResponse<RadioStationResponse> searchStations(StationSearchRequest request, User user) {
        int page = request.getPage();
        int size = request.getSize();

        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be zero or greater");
        }

        // RadioBrowser API only supports limit, not offset/page. Fetch a larger set to
        // support pagination.
        // Using 1000 as a reasonable limit that balances coverage with performance.
        // Note: This means pagination is limited to the first 1000 results from the
        // API.
        int apiLimit = 1000;
        List<RadioBrowserStationDto> apiStations = radioBrowserApiClient.searchStations(
                request.getName(),
                request.getCountry(),
                request.getLanguage(),
                request.getTags(),
                apiLimit);

        if (apiStations == null) {
            apiStations = List.of();
        }

        // Calculate pagination bounds
        int start = page * size;
        int end = Math.min(start + size, apiStations.size());

        List<RadioBrowserStationDto> paginatedStations = start < apiStations.size()
                ? apiStations.subList(start, end)
                : List.of();

        // Fetch favorites only for the current page to improve performance
        Set<String> currentPageUuids = paginatedStations.stream()
                .map(RadioBrowserStationDto::getStationUuid)
                .collect(Collectors.toSet());

        Set<String> favoriteStationUuids = resolveFavoriteUuids(currentPageUuids, user);
        final Set<String> finalFavoriteStationUuids = favoriteStationUuids;

        List<RadioStationResponse> responses = paginatedStations.stream()
                .map(dto -> radioStationMapper.toResponse(
                        dto,
                        finalFavoriteStationUuids.contains(dto.getStationUuid())))
                .collect(Collectors.toList());

        // Use actual number of results returned by API for totalElements/totalPages
        // This reflects the actual available results, not the requested page size
        int totalElements = apiStations.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return PageResponse.<RadioStationResponse>builder()
                .content(responses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1 || totalElements == 0)
                .build();
    }

    @Transactional(readOnly = true)
    public RadioStationResponse getStationById(Long id, User user) {
        if (id == null) {
            throw new IllegalArgumentException("Station ID cannot be null");
        }

        RadioStation station = radioStationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Station not found with id: " + id));

        boolean isFavorite = favoriteRepository.existsByUserAndRadioStation(user, station);

        return radioStationMapper.toResponse(station, isFavorite);
    }

    @Transactional(readOnly = true)
    public PageResponse<RadioStationResponse> getSavedStations(int page, int size, User user) {
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be zero or greater");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RadioStation> stationsPage = radioStationRepository.findAll(pageable);

        Set<String> currentPageStationUuids = stationsPage.getContent().stream()
                .map(RadioStation::getStationUuid)
                .collect(Collectors.toSet());

        Set<String> favoriteStationUuids = resolveFavoriteUuids(currentPageStationUuids, user);

        List<RadioStationResponse> responses = stationsPage.getContent().stream()
                .map(station -> radioStationMapper.toResponse(
                        station,
                        favoriteStationUuids.contains(station.getStationUuid())))
                .collect(Collectors.toList());

        return PageResponse.<RadioStationResponse>builder()
                .content(responses)
                .page(page)
                .size(size)
                .totalElements(stationsPage.getTotalElements())
                .totalPages(stationsPage.getTotalPages())
                .first(stationsPage.isFirst())
                .last(stationsPage.isLast())
                .build();
    }

    @Transactional
    public void saveStations(List<String> stationUuids) {
        for (String stationUuid : stationUuids) {
            try {
                saveSingleStation(stationUuid);
            } catch (Exception e) {
                log.error("Failed to save station with UUID: {}", stationUuid, e);
                // Exception is logged but not re-thrown to allow batch to continue
                // Each station's transaction has already rolled back independently
            }
        }
    }

    /**
     * Saves a single radio station by looking it up from the RadioBrowser API,
     * converting it via the mapper, and either updating an existing station or
     * inserting a new one.
     * <p>
     * This method runs in its own transaction (REQUIRES_NEW) so that failures
     * for one station do not affect other stations in a batch operation.
     * </p>
     *
     * @param stationUuid the UUID of the station to save
     * @throws RuntimeException if the station cannot be found in the API, mapper
     *                          conversion fails, or repository operations fail
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveSingleStation(String stationUuid) {
        RadioBrowserStationDto apiStation = radioBrowserApiClient.getStationByUuid(stationUuid);

        if (apiStation == null) {
            log.error("Station with UUID {} not found in Radio Browser API", stationUuid);
            throw new IllegalArgumentException("Station with UUID " + stationUuid + " not found in Radio Browser API");
        }

        radioStationRepository.findByStationUuid(stationUuid)
                .ifPresentOrElse(
                        existing -> {
                            RadioStation updated = radioStationMapper.toEntity(apiStation);
                            if (updated != null) {
                                updated.setId(existing.getId());
                                updated.setCreatedAt(existing.getCreatedAt());
                                radioStationRepository.save(updated);
                                log.debug("Updated station with UUID: {}", stationUuid);
                            } else {
                                log.error("Mapper returned null for station with UUID: {}", stationUuid);
                                throw new IllegalStateException(
                                        "Mapper returned null for station with UUID: " + stationUuid);
                            }
                        },
                        () -> {
                            RadioStation newStation = radioStationMapper.toEntity(apiStation);
                            if (newStation != null) {
                                radioStationRepository.save(newStation);
                                log.debug("Saved new station with UUID: {}", stationUuid);
                            } else {
                                log.error("Mapper returned null for station with UUID: {}", stationUuid);
                                throw new IllegalStateException(
                                        "Mapper returned null for station with UUID: " + stationUuid);
                            }
                        });
    }
}
