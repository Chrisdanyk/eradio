package cg.xis.eradio.controller;

import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.dto.request.StationSearchRequest;
import cg.xis.eradio.dto.response.PageResponse;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.service.RadioStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Tag(name = "Radio Stations", description = "Endpoints for searching and retrieving radio stations")
@SecurityRequirement(name = "Bearer Authentication")
public class RadioStationController {

    private final RadioStationService radioStationService;

    @Operation(summary = "Search radio stations", description = "Search and filter radio stations by name, country, language, or tags")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<RadioStationResponse>> searchStations(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        StationSearchRequest request = new StationSearchRequest();
        request.setName(name);
        request.setCountry(country);
        request.setLanguage(language);
        request.setTags(tags);
        request.setPage(page);
        request.setSize(size);

        PageResponse<RadioStationResponse> response = radioStationService.searchStations(request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get station by ID", description = "Retrieve a specific radio station by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<RadioStationResponse> getStationById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        RadioStationResponse response = radioStationService.getStationById(id, user);
        return ResponseEntity.ok(response);
    }
}

