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

    @Transactional(readOnly = true)
    public PageResponse<RadioStationResponse> searchStations(StationSearchRequest request, User user) {
        List<RadioBrowserStationDto> apiStations = radioBrowserApiClient.searchStations(
                request.getName(),
                request.getCountry(),
                request.getLanguage(),
                request.getTags(),
                100 // Limit for API call
        );

        Set<String> favoriteStationUuids = favoriteRepository.findByUser(user, Pageable.unpaged())
                .getContent()
                .stream()
                .map(fav -> {
                    RadioStation station = fav.getRadioStation();
                    return station != null ? station.getStationUuid() : null;
                })
                .filter(uuid -> uuid != null)
                .collect(Collectors.toSet());

        int page = request.getPage();
        int size = request.getSize();

        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be zero or greater");
        }

        int start = page * size;
        int end = Math.min(start + size, apiStations.size());

        List<RadioBrowserStationDto> paginatedStations = start < apiStations.size()
                ? apiStations.subList(start, end)
                : List.of();

        List<RadioStationResponse> responses = paginatedStations.stream()
                .map(dto -> radioStationMapper.toResponse(
                        dto,
                        favoriteStationUuids.contains(dto.getStationUuid())))
                .collect(Collectors.toList());

        int totalElements = apiStations.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<RadioStationResponse>builder()
                .content(responses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
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

        Set<Long> favoriteStationIds = favoriteRepository.findByUser(user, Pageable.unpaged())
                .getContent()
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
            try {
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
            } catch (Exception e) {
                log.error("Error saving station with UUID {}: {}", stationUuid, e.getMessage(), e);
            }
        }
    }
}
