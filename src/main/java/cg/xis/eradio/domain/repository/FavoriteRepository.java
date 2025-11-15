package cg.xis.eradio.domain.repository;

import cg.xis.eradio.domain.entity.Favorite;
import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    void deleteByUserAndRadioStation(User user, RadioStation radioStation);
}
