package uwr.ms.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uwr.ms.model.entity.ExpenseEntity;
import uwr.ms.model.entity.ExpenseParticipantEntity;

import java.util.List;

public interface ExpenseParticipantEntityRepository extends JpaRepository<ExpenseParticipantEntity, Long> {
    List<ExpenseParticipantEntity> findAllByParticipantUsernameAndExpenseTripId(String username, Long TripId);
}
