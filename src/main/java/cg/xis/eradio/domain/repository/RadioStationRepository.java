package cg.xis.eradio.domain.repository;

import cg.xis.eradio.domain.entity.RadioStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RadioStationRepository extends JpaRepository<RadioStation, Long> {
    Optional<RadioStation> findByStationUuid(String stationUuid);

    /**
     * Finds radio stations by their UUIDs.
     * <p>
     * This method performs an IN query which can become inefficient with very large
     * collections.
     * It is recommended to limit the batch size to a maximum of 500 UUIDs per call
     * to avoid
     * performance issues and potential database query limitations.
     * </p>
     *
     * @param stationUuids collection of station UUIDs to search for
     * @return list of matching RadioStation entities
     */
    List<RadioStation> findByStationUuidIn(Collection<String> stationUuids);

    @Query("SELECT rs FROM RadioStation rs WHERE " +
            "(:name IS NULL OR :name = '' OR LOWER(rs.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:country IS NULL OR :country = '' OR LOWER(rs.country) LIKE LOWER(CONCAT('%', :country, '%'))) AND " +
            "(:language IS NULL OR :language = '' OR LOWER(rs.language) LIKE LOWER(CONCAT('%', :language, '%'))) AND " +
            "(:tags IS NULL OR :tags = '' OR LOWER(rs.tags) LIKE LOWER(CONCAT('%', :tags, '%')))")
    Page<RadioStation> searchStations(
            @Param("name") String name,
            @Param("country") String country,
            @Param("language") String language,
            @Param("tags") String tags,
            Pageable pageable);
}
