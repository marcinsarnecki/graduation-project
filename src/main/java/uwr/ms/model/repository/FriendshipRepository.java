package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uwr.ms.model.entity.FriendshipEntity;
import uwr.ms.constant.FriendshipStatus;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {
    @Query("SELECT f FROM FriendshipEntity f WHERE f.requester.username = :username OR f.addressee.username = :username")
    List<FriendshipEntity> findByAddresseeUsernameOrRequesterUsername(@Param("username") String username);

    List<FriendshipEntity> findByAddresseeUsernameAndStatus(String addresseeUsername, FriendshipStatus status);
    Optional<FriendshipEntity> findByRequesterUsernameAndAddresseeUsernameAndStatus(String requesterUsername, String addresseeUsername, FriendshipStatus requested);
}
