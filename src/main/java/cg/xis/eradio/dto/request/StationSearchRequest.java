package cg.xis.eradio.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StationSearchRequest {
    private String name;
    private String country;
    private String language;
    private String tags;

    @Min(value = 0, message = "Page index must be zero or greater")
    private int page = 0;

    @Positive(message = "Page size must be greater than zero")
    private int size = 20;
}
