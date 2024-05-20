package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uwr.ms.model.entity.ExpenseEntity;

import java.util.List;

public interface ExpenseEntityRepository extends JpaRepository<ExpenseEntity, Long> {
    List<ExpenseEntity> findAllByTripId(Long tripId);

    List<ExpenseEntity> findAllByPayerUsernameAndTripId(String username, Long tripId);
    void deleteAllByTripId(Long tripId);
}
