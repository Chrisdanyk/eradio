package cg.xis.eradio.controller;

import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.dto.response.PageResponse;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Endpoints for managing favorite radio stations")
@SecurityRequirement(name = "Bearer Authentication")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Add station to favorites", description = "Add a radio station to the user's favorites list")
    @PostMapping("/{stationId}")
    public ResponseEntity<Void> addFavorite(
            @PathVariable Long stationId,
            @AuthenticationPrincipal User user) {
        favoriteService.addFavorite(stationId, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove station from favorites", description = "Remove a radio station from the user's favorites list")
    @DeleteMapping("/{stationId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long stationId,
            @AuthenticationPrincipal User user) {
        favoriteService.removeFavorite(stationId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user's favorites", description = "Retrieve paginated list of the user's favorite radio stations")
    @GetMapping
    public ResponseEntity<PageResponse<RadioStationResponse>> getFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        PageResponse<RadioStationResponse> response = favoriteService.getFavorites(user, page, size);
        return ResponseEntity.ok(response);
    }
}

