package uwr.ms.model.repository;

import org.springframework.data.repository.CrudRepository;
import uwr.ms.model.entity.EventEntity;

import java.util.List;

public interface EventEntityRepository extends CrudRepository<EventEntity, Long> {
    void deleteAllByTripId(Long tripId);

    List<EventEntity> findAllByTripId(Long tripId);
}
