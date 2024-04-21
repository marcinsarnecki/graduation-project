package uwr.ms.model.repository;

import org.springframework.data.repository.CrudRepository;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripInvitationEntity;
import uwr.ms.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface TripInvitationRepository extends CrudRepository<TripInvitationEntity, Long> {

    Optional<TripInvitationEntity> findByTripAndReceiver(TripEntity trip, UserEntity invitedUser);

    List<TripInvitationEntity> findByReceiverUsername(String username);
}
