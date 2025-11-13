package cg.xis.eradio.infrastructure.mapper;

import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.dto.response.RadioStationResponse;
import cg.xis.eradio.infrastructure.client.dto.RadioBrowserStationDto;
import org.springframework.stereotype.Component;

@Component
public class RadioStationMapper {

    public RadioStation toEntity(RadioBrowserStationDto dto) {
        if (dto == null) {
            return null;
        }

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

        return RadioStation.builder()
            .stationUuid(dto.getStationUuid())
            .name(dto.getName())
            .url(url)
            .urlResolved(urlResolved)
            .homepage(dto.getHomepage())
            .favicon(dto.getFavicon())
            .tags(tags)
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
}

