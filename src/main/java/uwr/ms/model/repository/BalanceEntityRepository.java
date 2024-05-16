package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uwr.ms.model.entity.BalanceEntity;

import java.util.List;

public interface BalanceEntityRepository extends JpaRepository<BalanceEntity, Long> {
    List<BalanceEntity> findAllByTripId(Long tripId);
    void deleteAllByTripId(Long tripId);
}
