package cg.xis.eradio.service;

import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.domain.repository.FavoriteRepository;
import cg.xis.eradio.domain.repository.RadioStationRepository;
import cg.xis.eradio.dto.request.StationSearchRequest;
import cg.xis.eradio.dto.response.PageResponse;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.infrastructure.client.RadioBrowserApiClient;
import cg.xis.eradio.infrastructure.mapper.RadioStationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RadioStationService {

    private final RadioStationRepository radioStationRepository;
    private final FavoriteRepository favoriteRepository;
    private final RadioBrowserApiClient radioBrowserApiClient;
    private final RadioStationMapper radioStationMapper;

    @Transactional
    public PageResponse<RadioStationResponse> searchStations(StationSearchRequest request, User user) {
        // First, try to fetch from external API and sync to database
        syncStationsFromApi(request);

        // Then search in database
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<RadioStation> stationsPage = radioStationRepository.searchStations(
            request.getName(),
            request.getCountry(),
            request.getLanguage(),
            request.getTags(),
            pageable
        );

        // Get user's favorite station IDs
        Set<Long> favoriteStationIds = favoriteRepository.findByUser(user, Pageable.unpaged())
            .getContent()
            .stream()
            .map(fav -> {
                RadioStation station = fav.getRadioStation();
                return station != null ? station.getId() : null;
            })
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        // Map to response with favorite status
        List<RadioStationResponse> responses = stationsPage.getContent().stream()
            .map(station -> radioStationMapper.toResponse(
                station,
                favoriteStationIds.contains(station.getId())
            ))
            .collect(Collectors.toList());

        return PageResponse.<RadioStationResponse>builder()
            .content(responses)
            .page(stationsPage.getNumber())
            .size(stationsPage.getSize())
            .totalElements(stationsPage.getTotalElements())
            .totalPages(stationsPage.getTotalPages())
            .first(stationsPage.isFirst())
            .last(stationsPage.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public RadioStationResponse getStationById(Long id, User user) {
        RadioStation station = radioStationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Station not found with id: " + id));

        boolean isFavorite = favoriteRepository.existsByUserAndRadioStation(user, station);

        return radioStationMapper.toResponse(station, isFavorite);
    }

    private void syncStationsFromApi(StationSearchRequest request) {
        try {
            List<cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto> apiStations =
                radioBrowserApiClient.searchStations(
                    request.getName(),
                    request.getCountry(),
                    request.getLanguage(),
                    request.getTags(),
                    100 // Limit for API call
                );

            for (cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto apiStation : apiStations) {
                try {
                    radioStationRepository.findByStationUuid(apiStation.getStationUuid())
                        .ifPresentOrElse(
                            existing -> {
                                try {
                                    // Update existing station
                                    RadioStation updated = radioStationMapper.toEntity(apiStation);
                                    if (updated != null) {
                                        updated.setId(existing.getId());
                                        updated.setCreatedAt(existing.getCreatedAt());
                                        radioStationRepository.save(updated);
                                    }
                                } catch (Exception e) {
                                    log.warn("Error updating station {}: {}", apiStation.getStationUuid(), e.getMessage());
                                }
                            },
                            () -> {
                                try {
                                    // Save new station
                                    RadioStation newStation = radioStationMapper.toEntity(apiStation);
                                    if (newStation != null) {
                                        radioStationRepository.save(newStation);
                                    }
                                } catch (Exception e) {
                                    log.warn("Error saving new station {}: {}", apiStation.getStationUuid(), e.getMessage());
                                }
                            }
                        );
                } catch (Exception e) {
                    log.warn("Error processing station {}: {}", apiStation.getStationUuid(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error syncing stations from API", e);
        }
    }
}

