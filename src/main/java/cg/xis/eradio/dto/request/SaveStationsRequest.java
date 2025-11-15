package cg.xis.eradio.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveStationsRequest {

    @NotEmpty(message = "Station UUIDs list cannot be empty")
    private List<String> stationUuids;
}

