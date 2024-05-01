package uwr.ms.model.repository;

import org.springframework.data.repository.CrudRepository;
import uwr.ms.model.entity.EventEntity;

public interface EventEntityRepository extends CrudRepository<EventEntity, Long> {
}
