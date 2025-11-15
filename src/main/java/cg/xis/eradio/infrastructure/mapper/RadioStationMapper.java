package cg.xis.eradio.infrastructure.mapper;

import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto;
import org.springframework.stereotype.Component;

@Component
public class RadioStationMapper {

    /**
     * Value object holding normalized/truncated string fields from
     * RadioBrowserStationDto
     */
    private record NormalizedDtoFields(String tags, String url, String urlResolved) {
    }

    /**
     * Normalizes and truncates string fields from RadioBrowserStationDto to ensure
     * consistent field length limits across all mapping operations.
     *
     * @param dto the source DTO
     * @return normalized fields with proper truncation applied
     */
    private NormalizedDtoFields normalizeDto(RadioBrowserStationDto dto) {
        // Truncate tags if too long (max 2000 characters)
        String tags = dto.getTags();
        if (tags != null && tags.length() > 2000) {
            tags = tags.substring(0, 2000);
        }

        // Truncate URLs if too long (max 8000 characters)
        String url = dto.getUrl();
        if (url != null && url.length() > 8000) {
            url = url.substring(0, 8000);
        }

        String urlResolved = dto.getUrlResolved();
        if (urlResolved != null && urlResolved.length() > 8000) {
            urlResolved = urlResolved.substring(0, 8000);
        }

        return new NormalizedDtoFields(tags, url, urlResolved);
    }

    public RadioStation toEntity(RadioBrowserStationDto dto) {
        if (dto == null) {
            return null;
        }

        NormalizedDtoFields normalized = normalizeDto(dto);

        return RadioStation.builder()
                .stationUuid(dto.getStationUuid())
                .name(dto.getName())
                .url(normalized.url())
                .urlResolved(normalized.urlResolved())
                .homepage(dto.getHomepage())
                .favicon(dto.getFavicon())
                .tags(normalized.tags())
                .country(dto.getCountry())
                .countryCode(dto.getCountryCode())
                .state(dto.getState())
                .language(dto.getLanguage())
                .languageCodes(dto.getLanguageCodes())
                .votes(dto.getVotes())
                .codec(dto.getCodec())
                .bitrate(dto.getBitrate())
                .hls(dto.getHls() != null && dto.getHls() == 1)
                .lastCheckOk(dto.getLastCheckOk() != null && dto.getLastCheckOk() == 1)
                .build();
    }

    public RadioStationResponse toResponse(RadioStation station, boolean isFavorite) {
        if (station == null) {
            return null;
        }

        return RadioStationResponse.builder()
                .id(station.getId())
                .stationUuid(station.getStationUuid())
                .name(station.getName())
                .url(station.getUrl())
                .urlResolved(station.getUrlResolved())
                .homepage(station.getHomepage())
                .favicon(station.getFavicon())
                .tags(station.getTags())
                .country(station.getCountry())
                .countryCode(station.getCountryCode())
                .state(station.getState())
                .language(station.getLanguage())
                .languageCodes(station.getLanguageCodes())
                .votes(station.getVotes())
                .codec(station.getCodec())
                .bitrate(station.getBitrate())
                .hls(station.getHls())
                .lastCheckOk(station.getLastCheckOk())
                .isFavorite(isFavorite)
                .build();
    }

    /**
     * Maps RadioBrowserStationDto directly to RadioStationResponse (for search
     * results without DB save)
     */
    public RadioStationResponse toResponse(RadioBrowserStationDto dto, boolean isFavorite) {
        if (dto == null) {
            return null;
        }

        NormalizedDtoFields normalized = normalizeDto(dto);

        return RadioStationResponse.builder()
                .id(null) // No ID since not saved in DB yet
                .stationUuid(dto.getStationUuid())
                .name(dto.getName())
                .url(normalized.url())
                .urlResolved(normalized.urlResolved())
                .homepage(dto.getHomepage())
                .favicon(dto.getFavicon())
                .tags(normalized.tags())
                .country(dto.getCountry())
                .countryCode(dto.getCountryCode())
                .state(dto.getState())
                .language(dto.getLanguage())
                .languageCodes(dto.getLanguageCodes())
                .votes(dto.getVotes())
                .codec(dto.getCodec())
                .bitrate(dto.getBitrate())
                .hls(dto.getHls() != null && dto.getHls() == 1)
                .lastCheckOk(dto.getLastCheckOk() != null && dto.getLastCheckOk() == 1)
                .isFavorite(isFavorite)
                .build();
    }
}
