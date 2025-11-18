package cg.xis.eradio.controller;

import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.dto.response.RecommendationsResponse;
import cg.xis.eradio.service.AiRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recommendations", description = "AI-powered radio station recommendations based on user preferences")
@SecurityRequirement(name = "Bearer Authentication")
public class RecommendationsController {

    private final AiRecommendationService aiRecommendationService;

    @Operation(summary = "Get personalized recommendations", description = "Get AI-powered radio station recommendations based on the user's favorite stations. "
            +
            "The AI analyzes your listening preferences and suggests stations you might enjoy.")
    @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    @GetMapping
    public ResponseEntity<RecommendationsResponse> getRecommendations(
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 20, message = "Limit cannot exceed 20") @Positive(message = "Limit must be positive") int limit,
            @AuthenticationPrincipal User user) {

        RecommendationsResponse response = aiRecommendationService.getRecommendations(user, limit);
        return ResponseEntity.ok(response);
    }
}
