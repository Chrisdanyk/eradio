package cg.xis.eradio.service;

import cg.xis.eradio.domain.entity.Favorite;
import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.domain.repository.FavoriteRepository;
import cg.xis.eradio.domain.repository.RadioStationRepository;
import cg.xis.eradio.dto.response.PageResponse;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.infrastructure.mapper.RadioStationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RadioStationRepository radioStationRepository;
    private final RadioStationMapper radioStationMapper;

    @Transactional
    public void addFavorite(Long stationId, User user) {
        if (stationId == null) {
            throw new IllegalArgumentException("Station ID cannot be null");
        }

        // Validate station exists in DB (must be saved first)
        RadioStation station = radioStationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Station not found with id: " + stationId + ". Please save the station first."));

        // Upsert: if already exists, do nothing (idempotent)
        if (favoriteRepository.existsByUserAndRadioStation(user, station)) {
            return; // Already in favorites, no action needed
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .radioStation(station)
                .build();

        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long stationId, User user) {
        if (stationId == null) {
            throw new IllegalArgumentException("Station ID cannot be null");
        }
        RadioStation station = radioStationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Station not found with id: " + stationId + ". Please save the station first."));

        favoriteRepository.deleteByUserAndRadioStation(user, station);
    }

    @Transactional(readOnly = true)
    public PageResponse<RadioStationResponse> getFavorites(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favoritesPage = favoriteRepository.findByUser(user, pageable);

        List<RadioStationResponse> responses = favoritesPage.getContent().stream()
                .map(favorite -> {
                    RadioStation station = favorite.getRadioStation();
                    return station != null ? radioStationMapper.toResponse(station, true) : null;
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());

        return PageResponse.<RadioStationResponse>builder()
                .content(responses)
                .page(favoritesPage.getNumber())
                .size(favoritesPage.getSize())
                .totalElements(favoritesPage.getTotalElements())
                .totalPages(favoritesPage.getTotalPages())
                .first(favoritesPage.isFirst())
                .last(favoritesPage.isLast())
                .build();
    }
}