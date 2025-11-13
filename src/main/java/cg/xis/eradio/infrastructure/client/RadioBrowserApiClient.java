package cg.xis.eradio.infrastructure.client;

import cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RadioBrowserApiClient {

    @Value("${radio-browser.api.base-url:https://de1.api.radio-browser.info}")
    private String baseUrl;

    private final RestClient.Builder restClientBuilder;

    private RestClient getRestClient() {
        String url = baseUrl != null ? baseUrl : "https://de1.api.radio-browser.info";
        return restClientBuilder
            .baseUrl(url)
            .build();
    }

    public List<RadioBrowserStationDto> searchStations(String country, Integer limit) {
        try {
            String url = "/json/stations/search?country=" + country + "&limit=" + limit;
            RadioBrowserStationDto[] stations = getRestClient().get()
                .uri(url)
                .retrieve()
                .body(RadioBrowserStationDto[].class);

            return stations != null ? Arrays.asList(stations) : List.of();
        } catch (Exception e) {
            log.error("Error fetching stations from Radio Browser API", e);
            return List.of();
        }
    }

    public List<RadioBrowserStationDto> searchStations(String name, String country, String language, String tags, Integer limit) {
        try {
            StringBuilder urlBuilder = new StringBuilder("/json/stations/search?limit=" + limit);

            if (name != null && !name.isBlank()) {
                urlBuilder.append("&name=").append(name);
            }
            if (country != null && !country.isBlank()) {
                urlBuilder.append("&country=").append(country);
            }
            if (language != null && !language.isBlank()) {
                urlBuilder.append("&language=").append(language);
            }
            if (tags != null && !tags.isBlank()) {
                urlBuilder.append("&tags=").append(tags);
            }

            String uri = urlBuilder.toString();
            if (uri == null) {
                return List.of();
            }
            RadioBrowserStationDto[] stations = getRestClient().get()
                .uri(uri)
                .retrieve()
                .body(RadioBrowserStationDto[].class);

            return stations != null ? Arrays.asList(stations) : List.of();
        } catch (Exception e) {
            log.error("Error fetching stations from Radio Browser API", e);
            return List.of();
        }
    }
}

