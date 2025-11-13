package cg.xis.eradio.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "radio_stations", indexes = {
        @Index(name = "idx_station_uuid", columnList = "stationUuid", unique = true),
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_country", columnList = "country")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadioStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String stationUuid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(nullable = false, length = 8000)
    private String url;

    @Column(length = 8000)
    private String urlResolved;

    @Column(length = 500)
    private String homepage;

    @Column(length = 500)
    private String favicon;

    @Column(length = 2000, columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String country;

    @Column(length = 2)
    private String countryCode;

    @Column(columnDefinition = "TEXT")
    private String state;

    @Column(columnDefinition = "TEXT")
    private String language;

    @Column(columnDefinition = "TEXT")
    private String languageCodes;

    private Integer votes;

    @Column(columnDefinition = "TEXT")
    private String codec;

    private Integer bitrate;

    private Boolean hls;

    private Boolean lastCheckOk;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
