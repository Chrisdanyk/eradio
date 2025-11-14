package cg.xis.eradio.service;

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

        // Calculate pagination bounds
        int start = page * size;
        int end = Math.min(start + size, apiStations.size());

        List<RadioBrowserStationDto> paginatedStations = start < apiStations.size()
                ? apiStations.subList(start, end)
                : List.of();

        // Fetch favorites only for the current page to improve performance
        Set<String> favoriteStationUuids;
        if (paginatedStations.isEmpty()) {
            favoriteStationUuids = Set.of();
        } else {
            Set<String> currentPageUuids = paginatedStations.stream()
                    .map(RadioBrowserStationDto::getStationUuid)
                    .collect(Collectors.toSet());

            List<RadioStation> favoriteStations = radioStationRepository.findByStationUuidIn(currentPageUuids);
            if (favoriteStations.isEmpty()) {
                favoriteStationUuids = Set.of();
            } else {
                Set<Long> favoriteStationIds = favoriteStations.stream()
                        .map(RadioStation::getId)
                        .collect(Collectors.toSet());

                Set<String> uuids = favoriteRepository.findByUserAndRadioStationIdIn(user, favoriteStationIds)
                        .stream()
                        .map(fav -> {
                            RadioStation station = fav.getRadioStation();
                            return station != null ? station.getStationUuid() : null;
                        })
                        .filter(uuid -> uuid != null)
                        .collect(Collectors.toSet());
                favoriteStationUuids = uuids;
            }
        }
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

        Set<Long> currentPageStationIds = stationsPage.getContent().stream()
                .map(RadioStation::getId)
                .collect(Collectors.toSet());

        Set<Long> favoriteStationIds = favoriteRepository.findByUserAndRadioStationIdIn(user, currentPageStationIds)
                .stream()
                .map(fav -> {
                    RadioStation station = fav.getRadioStation();
                    return station != null ? station.getId() : null;
                })
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<RadioStationResponse> responses = stationsPage.getContent().stream()
                .map(station -> radioStationMapper.toResponse(
                        station,
                        favoriteStationIds.contains(station.getId())))
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
            RadioBrowserStationDto apiStation = radioBrowserApiClient.getStationByUuid(stationUuid);

            if (apiStation == null) {
                log.warn("Station with UUID {} not found in Radio Browser API", stationUuid);
                continue;
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
                                }
                            },
                            () -> {
                                RadioStation newStation = radioStationMapper.toEntity(apiStation);
                                if (newStation != null) {
                                    radioStationRepository.save(newStation);
                                    log.debug("Saved new station with UUID: {}", stationUuid);
                                }
                            });
        }
    }
}
