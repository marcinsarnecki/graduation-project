package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uwr.ms.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserEntityRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
    @Query("SELECT u FROM UserEntity u " +
            "WHERE u.username IN " +
            " (SELECT f.addressee.username FROM FriendshipEntity f WHERE f.requester.username = :username AND f.status = 'ACCEPTED') " +
            "OR u.username IN " +
            " (SELECT f.requester.username FROM FriendshipEntity f WHERE f.addressee.username = :username AND f.status = 'ACCEPTED')")
    List<UserEntity> findFriendsByUsername(@Param("username") String username);

    @Query("SELECT p.user FROM TripParticipantEntity p WHERE p.trip.id = :tripId AND NOT p.user.username = :username " +
            "AND p.user.username NOT IN (" +
            "SELECT f.addressee.username FROM FriendshipEntity f WHERE f.requester.username = :username" +
            ") AND p.user.username NOT IN (" +
            "SELECT f.requester.username FROM FriendshipEntity f WHERE f.addressee.username = :username" +
            ")")
    List<UserEntity> findPotentialFriendsAmongParticipants(@Param("tripId") Long tripId, @Param("username") String username);
}
