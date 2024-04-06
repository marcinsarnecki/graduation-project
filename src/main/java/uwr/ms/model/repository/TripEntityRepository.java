package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uwr.ms.model.entity.TripEntity;

public interface TripEntityRepository extends JpaRepository<TripEntity, Long> {
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TripEntity t JOIN t.participants p WHERE t.name = :name AND p.user.username = :username AND p.role = uwr.ms.constant.TripParticipantRole.OWNER")
    boolean existsByNameAndOwnerUsername(String name, String username);
}
