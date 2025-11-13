package cg.xis.eradio.dto.request;

import lombok.Data;

@Data
public class StationSearchRequest {
    private String name;
    private String country;
    private String language;
    private String tags;
    private int page = 0;
    private int size = 20;
}

