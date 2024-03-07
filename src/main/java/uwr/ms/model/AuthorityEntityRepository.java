package uwr.ms.model;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AuthorityEntityRepository extends CrudRepository<AuthorityEntity, Long> {
    Optional<AuthorityEntity> findByName(String authority);
}
