package cg.xis.eradio.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadioBrowserStationDto {
    @JsonProperty("stationuuid")
    private String stationUuid;

    private String name;

    private String url;

    @JsonProperty("url_resolved")
    private String urlResolved;

    private String homepage;

    private String favicon;

    private String tags;

    private String country;

    @JsonProperty("countrycode")
    private String countryCode;

    private String state;

    private String language;

    @JsonProperty("languagecodes")
    private String languageCodes;

    private Integer votes;

    private String codec;

    private Integer bitrate;

    @JsonProperty("hls")
    private Integer hls;

    @JsonProperty("lastcheckok")
    private Integer lastCheckOk;
}

