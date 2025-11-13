package cg.xis.eradio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioStationResponse {
    private Long id;
    private String stationUuid;
    private String name;
    private String url;
    private String urlResolved;
    private String homepage;
    private String favicon;
    private String tags;
    private String country;
    private String countryCode;
    private String state;
    private String language;
    private String languageCodes;
    private Integer votes;
    private String codec;
    private Integer bitrate;
    private Boolean hls;
    private Boolean lastCheckOk;
    private Boolean isFavorite;
}

