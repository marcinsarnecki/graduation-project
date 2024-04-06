package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uwr.ms.constant.TripParticipantRole;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface TripParticipantEntityRepository extends JpaRepository<TripParticipantEntity, Long> {
    List<TripParticipantEntity> findByTripId(Long tripId);

    List<TripParticipantEntity> findByUserUsername(String username);

    Optional<TripParticipantEntity> findByTripAndUser(TripEntity trip, UserEntity user);

    void deleteByUser(UserEntity user);

    List<TripParticipantEntity> findByUserAndRole(UserEntity user, TripParticipantRole owner);
}

