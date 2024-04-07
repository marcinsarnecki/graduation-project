package uwr.ms.model.repository;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uwr.ms.constant.LoginProvider;
import uwr.ms.model.entity.UserEntity;

@DataJpaTest
@ActiveProfiles("postgres")
class UserEntityRepositoryTest {

    @Autowired
    UserEntityRepository userEntityRepository;

    @Test
    void testQuery() {
        userEntityRepository.save(new UserEntity("user1", "abcd1234!", "user1@example.com", "user1", LoginProvider.APP, ""));
        Assertions.assertTrue(userEntityRepository.findByUsername("user1").isPresent());
    }
}