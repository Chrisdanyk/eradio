package cg.xis.eradio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationsResponse {
    private List<RadioStationResponse> recommendations;
    private String reason; // AI-generated explanation for why these stations were recommended
}

