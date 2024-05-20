package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uwr.ms.constant.TripParticipantRole;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface TripParticipantEntityRepository extends JpaRepository<TripParticipantEntity, Long> {
    Optional<TripParticipantEntity> findByTripAndUser(TripEntity trip, UserEntity user);

    List<TripParticipantEntity> findByUserAndRole(UserEntity user, TripParticipantRole role);
    @Query("SELECT DISTINCT t FROM TripParticipantEntity tp JOIN tp.trip t WHERE tp.user.username = :username")
    List<TripEntity> findDistinctTripsByUserUsername(@Param("username") String username);

    void deleteById(Long id);

    Optional<TripParticipantEntity> findByUserUsername(String participantUsername);

    void deleteAllByTripId(Long tripId);

    List<TripParticipantEntity> findAllByTripId(Long tripId);
}

