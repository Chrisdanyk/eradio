package cg.xis.eradio.infrastructure.client;

import cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            String encodedCountry = country != null ? URLEncoder.encode(country, StandardCharsets.UTF_8) : "";
            String url = "/json/stations/search?country=" + encodedCountry +
                    (limit != null ? "&limit=" + limit : "");
            RadioBrowserStationDto[] stations = getRestClient().get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("Error response from Radio Browser API: {} {}", response.getStatusCode(),
                                response.getStatusText());
                        throw new RestClientException("Radio Browser API returned error: " + response.getStatusCode());
                    })
                    .body(RadioBrowserStationDto[].class);

            return stations != null ? Arrays.asList(stations) : List.of();
        } catch (RestClientException e) {
            log.error("Error fetching stations from Radio Browser API: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching stations from Radio Browser API", e);
            return List.of();
        }
    }

    public List<RadioBrowserStationDto> searchStations(String name, String country, String language, String tags,
            Integer limit) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/json/stations/search");

            if (name != null && !name.isBlank()) {
                uriBuilder.queryParam("name", name);
            }
            if (country != null && !country.isBlank()) {
                uriBuilder.queryParam("country", country);
            }
            if (language != null && !language.isBlank()) {
                uriBuilder.queryParam("language", language);
            }
            if (tags != null && !tags.isBlank()) {
                uriBuilder.queryParam("tags", tags);
            }
            if (limit != null) {
                uriBuilder.queryParam("limit", limit);
            }

            String uri = uriBuilder.toUriString();
            RadioBrowserStationDto[] stations = getRestClient().get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("Error response from Radio Browser API: {} {}", response.getStatusCode(),
                                response.getStatusText());
                        throw new RestClientException("Radio Browser API returned error: " + response.getStatusCode());
                    })
                    .body(RadioBrowserStationDto[].class);

            return stations != null ? Arrays.asList(stations) : List.of();
        } catch (RestClientException e) {
            log.error("Error fetching stations from Radio Browser API: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching stations from Radio Browser API", e);
            return List.of();
        }
    }

    public RadioBrowserStationDto getStationByUuid(String stationUuid) {
        if (stationUuid == null || stationUuid.isBlank()) {
            log.warn("Attempted to fetch station with null or blank UUID");
            return null;
        }

        try {
            String url = "/json/stations/byuuid/" + stationUuid;
            RadioBrowserStationDto[] stations = getRestClient().get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("Error response from Radio Browser API for UUID {}: {} {}",
                                stationUuid, response.getStatusCode(), response.getStatusText());
                        throw new RestClientException("Radio Browser API returned error: " + response.getStatusCode());
                    })
                    .body(RadioBrowserStationDto[].class);

            if (stations != null && stations.length > 0) {
                return stations[0];
            }
            return null;
        } catch (RestClientException e) {
            log.error("Error fetching station by UUID {} from Radio Browser API: {}", stationUuid, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching station by UUID {} from Radio Browser API", stationUuid, e);
            return null;
        }
    }
}
