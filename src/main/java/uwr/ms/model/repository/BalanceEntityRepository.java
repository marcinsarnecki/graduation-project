package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uwr.ms.model.entity.BalanceEntity;

public interface BalanceEntityRepository extends JpaRepository<BalanceEntity, Long> {
}
