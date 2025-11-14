package cg.xis.eradio.controller;

import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.dto.request.SaveStationsRequest;
import cg.xis.eradio.dto.request.StationSearchRequest;
import cg.xis.eradio.dto.response.PageResponse;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.service.RadioStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@Validated
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
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be zero or greater") int page,
            @RequestParam(defaultValue = "20") @Positive(message = "Page size must be greater than zero") int size,
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

    @Operation(summary = "Get saved stations", description = "Retrieve a paginated list of all saved radio stations. Users can then select stations from this list to add to their favorites.")
    @GetMapping("/saved")
    public ResponseEntity<PageResponse<RadioStationResponse>> getSavedStations(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be zero or greater") int page,
            @RequestParam(defaultValue = "20") @Positive(message = "Page size must be greater than zero") int size,
            @AuthenticationPrincipal User user) {
        PageResponse<RadioStationResponse> response = radioStationService.getSavedStations(page, size, user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Save selected stations", description = "Save or update selected radio stations to the database. Only saves stations that the user is interested in.")
    @ApiResponse(responseCode = "204", description = "Stations successfully saved")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/save")
    public ResponseEntity<Void> saveStations(
            @Valid @RequestBody SaveStationsRequest request,
            @AuthenticationPrincipal User user) {
        radioStationService.saveStations(request.getStationUuids());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
