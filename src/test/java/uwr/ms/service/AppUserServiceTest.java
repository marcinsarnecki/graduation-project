package uwr.ms.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.LoginProvider;
import uwr.ms.controller.AppUserController;
import uwr.ms.exception.ValidationException;
import uwr.ms.model.AppUser;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.TripEntityRepository;
import uwr.ms.model.repository.TripParticipantEntityRepository;
import uwr.ms.model.repository.UserEntityRepository;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("postgres")
class AppUserServiceTest {
    @Autowired
    private AppUserService appUserService;

    @Autowired
    private TripService tripService;

    @Autowired
    private TripEntityRepository tripRepository;

    @Autowired
    private TripParticipantEntityRepository tripParticipantRepository;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String TEST_USER_USERNAME = "testUser";
    private final String TEST_USER_PASSWORD = "Password123!";
    private final String TEST_USER_EMAIL = "test@example.com";
    private final String TEST_USER_NAME = "Test User";
    private final LoginProvider TEST_LOGIN_PROVIDER = LoginProvider.APP;

    private final String ANOTHER_USER_USERNAME = "anotherUser";
    private final String ANOTHER_USER_PASSWORD = "AnotherPassword123!";
    private final String ANOTHER_USER_EMAIL = "another@example.com";
    private final String ANOTHER_USER_NAME = "Another User";


    private void createTestUser() {
        AppUser appUser = AppUser.builder().username(TEST_USER_USERNAME).password(TEST_USER_PASSWORD).name(TEST_USER_NAME).email(TEST_USER_EMAIL).provider(TEST_LOGIN_PROVIDER).build();
        appUserService.createUser(appUser);
    }

    private void createGenericUser() {
        AppUser appUser = AppUser.builder().username(ANOTHER_USER_USERNAME).password(ANOTHER_USER_PASSWORD).name(ANOTHER_USER_NAME).email(ANOTHER_USER_EMAIL).provider(TEST_LOGIN_PROVIDER).build();
        appUserService.createUser(appUser);
    }

    @Test
    void createUserSuccessfully() {
        createTestUser();
        UserEntity savedUser = userEntityRepository.findById(TEST_USER_USERNAME).orElseThrow();

        assertThat(savedUser.getUsername()).isEqualTo(TEST_USER_USERNAME);
        assertThat(passwordEncoder.matches(TEST_USER_PASSWORD, savedUser.getPassword())).isTrue();
        assertThat(savedUser.getEmail()).isEqualTo(TEST_USER_EMAIL);
        assertThat(savedUser.getName()).isEqualTo(TEST_USER_NAME);
        assertThat(savedUser.getProvider()).isEqualTo(TEST_LOGIN_PROVIDER);
    }

    @Test
    void changingPasswordSuccessfullyUpdatesUserPassword() {
        createTestUser();
        UserEntity savedUser = userEntityRepository.findById(TEST_USER_USERNAME).orElseThrow();
        String newPassword = "NewSecurePassword123!";

        appUserService.changePassword(savedUser.getUsername(), TEST_USER_PASSWORD, newPassword);

        UserEntity user = userEntityRepository.findById(TEST_USER_USERNAME).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
    }

    @Test
    void changePasswordWithIncorrectCurrentPasswordThrowsException() {
        createTestUser();
        UserEntity savedUser = userEntityRepository.findById(TEST_USER_USERNAME).orElseThrow();
        String incorrectCurrentPassword = "IncorrectPassword";
        String newPassword = "NewSecurePassword123!";

        assertThatThrownBy(() -> appUserService.changePassword(savedUser.getUsername(), incorrectCurrentPassword, newPassword)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid old password");

        assertThat(passwordEncoder.matches(TEST_USER_PASSWORD, savedUser.getPassword())).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordProvider")
    void createUserWithInvalidPasswordFormatThrowsValidationException(String password, String expectedErrorMessage) {
        AppUser appUser = AppUser.builder().username("newUser").password(password).email("newUser@example.com").name("New User").provider(LoginProvider.APP).build();

        Assertions.assertThatThrownBy(() -> appUserService.createUser(appUser)).isInstanceOf(ValidationException.class).extracting(ex -> ((ValidationException) ex).getErrors()).asList().contains(expectedErrorMessage);
    }

    private static Stream<Arguments> invalidPasswordProvider() {
        return Stream.of(Arguments.of("short", "Password must be at least 8 characters long"), Arguments.of("alllowercase", "Password must contain at least one uppercase letter"), Arguments.of("ALLUPPERCASE", "Password must contain at least one lowercase letter"), Arguments.of("NoDigitsHere!", "Password must contain at least one digit"));
    }

    @Test
    void updateUserProfileSuccessfully() {
        createTestUser();
        String newEmail = "newemail@example.com";
        String newName = "New Name";
        String newImageUrl = "http://newimage.url";

        AppUserController.EditProfileRequest editProfileRequest = new AppUserController.EditProfileRequest(newImageUrl, newName, newEmail);
        appUserService.updateUserProfile(TEST_USER_USERNAME, editProfileRequest);

        UserEntity updatedUser = userEntityRepository.findById(TEST_USER_USERNAME).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.getName()).isEqualTo(newName);
        assertThat(updatedUser.getImageUrl()).isEqualTo(newImageUrl);
    }

    @Test
    void deleteUserSuccessfullyWithTrips() {
        createTestUser();
        createGenericUser();

        TripEntity ownedTrip = new TripEntity("Test Trip");
        tripService.createTrip(ownedTrip, TEST_USER_USERNAME);

        TripEntity anotherUserTrip = new TripEntity("Another User's Trip");
        tripService.createTrip(anotherUserTrip, ANOTHER_USER_USERNAME);

        assertThat(userEntityRepository.existsById(TEST_USER_USERNAME)).isTrue();
        assertThat(tripRepository.existsById(ownedTrip.getId())).isTrue();
        assertThat(tripRepository.existsById(anotherUserTrip.getId())).isTrue();

        appUserService.deleteUser(TEST_USER_USERNAME);

        assertThat(userEntityRepository.existsById(TEST_USER_USERNAME)).isFalse();
        assertThat(userEntityRepository.existsById(ANOTHER_USER_USERNAME)).isTrue();
        assertThat(tripRepository.existsById(ownedTrip.getId())).isFalse();
        assertThat(tripRepository.existsById(anotherUserTrip.getId())).isTrue();
    }
}