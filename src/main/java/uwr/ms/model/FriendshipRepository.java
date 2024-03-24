package uwr.ms.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {
    List<FriendshipEntity> findByRequesterUsernameAndStatus(String requesterUsername, FriendshipStatus status);
    List<FriendshipEntity> findByAddresseeUsernameAndStatus(String addresseeUsername, FriendshipStatus status);
    Optional<FriendshipEntity> findByRequesterUsernameAndAddresseeUsernameAndStatus(String requesterUsername, String addresseeUsername, FriendshipStatus requested);
}
