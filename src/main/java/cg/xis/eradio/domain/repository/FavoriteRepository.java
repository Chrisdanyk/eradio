package cg.xis.eradio.domain.repository;

import cg.xis.eradio.domain.entity.Favorite;
import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
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
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndRadioStation(User user, RadioStation radioStation);

    boolean existsByUserAndRadioStation(User user, RadioStation radioStation);

    Page<Favorite> findByUser(User user, Pageable pageable);

    List<Favorite> findByUserAndRadioStationIdIn(User user, Collection<Long> stationIds);

    void deleteByUserAndRadioStation(User user, RadioStation station);

    /**
     * Find favorites by user with radio stations eagerly fetched.
     * Used for cache key generation to avoid lazy loading issues.
     */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.radioStation WHERE f.user = :user")
    List<Favorite> findByUserWithRadioStations(@Param("user") User user);
}
