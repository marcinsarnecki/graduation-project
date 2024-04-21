package uwr.ms.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uwr.ms.model.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserEntityRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT u FROM UserEntity u " +
            "where u.username in " +
            " (select f.addressee.username from FriendshipEntity f where f.requester.username = :username and f.status = 'ACCEPTED') " +
            "or u.username in " +
            "(select f.requester.username from FriendshipEntity f where f.addressee.username = :username and f.status = 'ACCEPTED')")
    Page<UserEntity> findFriendsByUsername(@Param("username") String username, Pageable pageable);
    @Query("SELECT u FROM UserEntity u " +
            "WHERE u.username IN " +
            " (SELECT f.addressee.username FROM FriendshipEntity f WHERE f.requester.username = :username AND f.status = 'ACCEPTED') " +
            "OR u.username IN " +
            " (SELECT f.requester.username FROM FriendshipEntity f WHERE f.addressee.username = :username AND f.status = 'ACCEPTED')")
    List<UserEntity> findFriendsByUsername(@Param("username") String username);
}
