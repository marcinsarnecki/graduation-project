package uwr.ms.model.repository;

import org.springframework.data.repository.CrudRepository;
import uwr.ms.model.entity.UserEntity;

import java.util.Optional;

public interface UserEntityRepository extends CrudRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
}
