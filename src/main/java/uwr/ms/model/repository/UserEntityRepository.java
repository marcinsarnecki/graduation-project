package uwr.ms.model.repository;

import org.springframework.data.repository.CrudRepository;
import uwr.ms.model.entity.UserEntity;

public interface UserEntityRepository extends CrudRepository<UserEntity, String> {
}
