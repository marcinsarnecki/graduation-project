package uwr.ms.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.EventType;
import uwr.ms.constant.FriendshipStatus;
import uwr.ms.constant.LoginProvider;
import uwr.ms.constant.Message;
import uwr.ms.controller.AppUserController;
import uwr.ms.controller.ExpensesController;
import uwr.ms.exception.ValidationException;
import uwr.ms.model.AppUser;
import uwr.ms.model.entity.EventEntity;
import uwr.ms.model.entity.ExpenseEntity;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("postgres")
class AppUserServiceTest {
    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private TripEntityRepository tripRepository;

    @Autowired
    private TripService tripService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private TripInvitationRepository tripInvitationRepository;

    @Autowired
    private EventEntityRepository eventEntityRepository;

    @Autowired
    private ExpenseEntityRepository expenseEntityRepository;

    @Autowired
    private ExpenseParticipantEntityRepository expenseParticipantEntityRepository;

    @Autowired
    private AppUserService userService;

    @Autowired
    private ExpensesService expensesService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    private UserEntity owner, user1, user2, user3;

    private EventEntity createEvent(String name, String location, LocalDate date, LocalTime time) {
        EventEntity event = new EventEntity();
        event.setEventName(name);
        event.setEventType(EventType.SINGLE);
        event.setLocation(location);
        event.setZoom(10);
        event.setDate(date);
        event.setTime(time);
        return event;
    }

    @BeforeEach
    void setup() {
        owner = new UserEntity("ownerUser", passwordEncoder.encode("Password1"), "owner@example.com", "Owner User", LoginProvider.APP, "");
        user1 = new UserEntity("user1", passwordEncoder.encode("Password2"), "user1@example.com", "User One", LoginProvider.APP, "");
        user2 = new UserEntity("user2", passwordEncoder.encode("Password3"), "user2@example.com", "User Two", LoginProvider.APP, "");
        user3 = new UserEntity("user3", passwordEncoder.encode("Password4"), "user3@example.com", "User Three", LoginProvider.APP, "");

        userRepository.saveAll(Arrays.asList(owner, user1, user2, user3));
    }

    @Test
    void createUserSuccessfully() {
        UserEntity savedUser = userRepository.findById("user1").orElseThrow();

        assertThat(savedUser.getUsername()).isEqualTo("user1");
        assertThat(passwordEncoder.matches("Password2", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getEmail()).isEqualTo("user1@example.com");
        assertThat(savedUser.getName()).isEqualTo("User One");
        assertThat(savedUser.getProvider()).isEqualTo(LoginProvider.APP);
    }

    @Test
    void changingPasswordSuccessfullyUpdatesUserPassword() {
        UserEntity savedUser = userRepository.findById("user1").orElseThrow();
        String newPassword = "NewSecurePassword123!";

        userService.changePassword(savedUser.getUsername(), "Password2", newPassword);

        UserEntity user = userRepository.findById("user1").orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
    }

    @Test
    void changePasswordWithIncorrectCurrentPasswordThrowsException() {
        UserEntity savedUser = userRepository.findById("user1").orElseThrow();
        String incorrectCurrentPassword = "IncorrectPassword";
        String newPassword = "NewSecurePassword123!";

        assertThatThrownBy(() -> userService.changePassword(savedUser.getUsername(), incorrectCurrentPassword, newPassword)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(Message.INVALID_OLD_PASSWORD.toString());

        assertThat(passwordEncoder.matches("Password2", savedUser.getPassword())).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordProvider")
    void createUserWithInvalidPasswordFormatThrowsValidationException(String password, String expectedErrorMessage) {
        AppUser appUser = AppUser.builder().username("newUser").password(password).email("newUser@example.com").name("New User").provider(LoginProvider.APP).build();

        Assertions.assertThatThrownBy(() -> userService.createUser(appUser)).isInstanceOf(ValidationException.class).extracting(ex -> ((ValidationException) ex).getErrors()).asList().contains(expectedErrorMessage);
    }

    private static Stream<Arguments> invalidPasswordProvider() {
        return Stream.of(
                Arguments.of("short", Message.PASSWORD_MIN_LENGTH.toString()),
                Arguments.of("alllowercase", Message.PASSWORD_UPPERCASE.toString()),
                Arguments.of("ALLUPPERCASE", Message.PASSWORD_LOWERCASE.toString()),
                Arguments.of("NoDigitsHere!", Message.PASSWORD_DIGIT.toString()));
    }

    @Test
    void updateUserProfileSuccessfully() {
        String newEmail = "newemail@example.com";
        String newName = "New Name";
        String newImageUrl = "http://newimage.url";

        AppUserController.EditProfileRequest editProfileRequest = new AppUserController.EditProfileRequest(newImageUrl, newName, newEmail);
        userService.updateUserProfile("user1", editProfileRequest);

        UserEntity updatedUser = userRepository.findById("user1").orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.getName()).isEqualTo(newName);
        assertThat(updatedUser.getImageUrl()).isEqualTo(newImageUrl);
    }

    @Test
    void deleteUserSuccessfully() {
        TripEntity trip1 = new TripEntity();
        trip1.setName("Trip 1");
        tripService.createTrip(trip1, owner.getUsername());

        TripEntity trip2 = new TripEntity();
        trip2.setName("Trip 2");
        tripService.createTrip(trip2, user1.getUsername());


        //owner is friend of users 1-3, user1 is friend of user2 and owner
        friendshipService.sendFriendRequest(owner.getUsername(), user1.getUsername());
        friendshipService.acceptFriendRequest(user1.getUsername(), friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(owner.getUsername(), user1.getUsername(), FriendshipStatus.REQUESTED).get().getId());

        friendshipService.sendFriendRequest(owner.getUsername(), user2.getUsername());
        friendshipService.acceptFriendRequest(user2.getUsername(), friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(owner.getUsername(), user2.getUsername(), FriendshipStatus.REQUESTED).get().getId());

        friendshipService.sendFriendRequest(owner.getUsername(), user3.getUsername());
        friendshipService.acceptFriendRequest(user3.getUsername(), friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(owner.getUsername(), user3.getUsername(), FriendshipStatus.REQUESTED).get().getId());

        friendshipService.sendFriendRequest(user1.getUsername(), user2.getUsername());
        friendshipService.acceptFriendRequest(user2.getUsername(), friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(user1.getUsername(), user2.getUsername(), FriendshipStatus.REQUESTED).get().getId());

        tripService.sendTripInvitation(trip1.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        tripService.sendTripInvitation(trip1.getId(), user2.getUsername(), owner.getUsername());
        Long invitationId2 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId2, user2.getUsername());

        tripService.sendTripInvitation(trip2.getId(), owner.getUsername(), user1.getUsername());
        Long invitationId3 = tripService.getTripInvitations(owner.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId3, owner.getUsername());

        tripService.sendTripInvitation(trip2.getId(), user2.getUsername(), user1.getUsername());
        Long invitationId4 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId4, user2.getUsername());

        EventEntity event1 = createEvent("Event 1", "Location 1", LocalDate.now(), LocalTime.now());
        EventEntity event2 = createEvent("Event 2", "Location 2", LocalDate.now(), LocalTime.now());
        EventEntity event3 = createEvent("Event 3", "Location 3", LocalDate.now(), LocalTime.now());
        tripService.addEventToTrip(event1, trip1);
        tripService.addEventToTrip(event2, trip1);
        tripService.addEventToTrip(event3, trip2);

        ExpensesController.ExpenseForm expenseForm1 = new ExpensesController.ExpenseForm(
                "Expense 1",
                1000,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername()),
                Arrays.asList(500, 500));
        ExpensesController.ExpenseForm expenseForm2 = new ExpensesController.ExpenseForm(
                "Expense 2",
                2000,
                LocalDate.now(),
                user1.getUsername(),
                Arrays.asList(owner.getUsername(), user2.getUsername()),
                Arrays.asList(1425, 575));
        expensesService.saveExpense(trip1.getId(), expenseForm1);
        expensesService.saveExpense(trip1.getId(), expenseForm2);

        ExpensesController.ExpenseForm expenseForm3 = new ExpensesController.ExpenseForm(
                "Expense 3",
                1333,
                LocalDate.now(),
                user1.getUsername(),
                Arrays.asList(owner.getUsername(), user2.getUsername()),
                Arrays.asList(1111, 222));
        expensesService.saveExpense(trip2.getId(), expenseForm3);

        assertThat(userRepository.findByUsername(owner.getUsername())).isPresent();
        assertThat(tripService.findAllTripsByUser(owner.getUsername())).hasSize(2);
        assertThat(eventEntityRepository.findAllByTripId(trip1.getId())).isNotEmpty();
        assertThat(expenseEntityRepository.findAllByTripId(trip1.getId())).isNotEmpty();
        assertThat(tripInvitationRepository.findByReceiverUsername(owner.getUsername())).isEmpty();
        assertThat(friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(owner.getUsername(), user1.getUsername(), FriendshipStatus.ACCEPTED)).isPresent();
        assertThat(friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(owner.getUsername(), user2.getUsername(), FriendshipStatus.ACCEPTED)).isPresent();
        assertThat(friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(owner.getUsername(), user3.getUsername(), FriendshipStatus.ACCEPTED)).isPresent();

        userService.deleteUser(owner.getUsername());

        assertThat(tripRepository.findAll()).hasSize(1);
        assertThat(tripService.findAllParticipantsByTripId(trip2.getId())).hasSize(2);
        assertThat(userRepository.findByUsername("ownerUser")).isEmpty();
        assertThat(tripService.findAllTripsByUser("ownerUser")).isEmpty();
        assertThat(eventEntityRepository.findAll()).hasSize(1);
        assertThat(expenseEntityRepository.findAllByTripId(trip1.getId())).isEmpty();
        assertThat(expenseParticipantEntityRepository.findAllByExpenseTripId(trip1.getId())).isEmpty();

        ExpenseEntity remainingExpense = expenseEntityRepository.findAllByTripId(trip2.getId()).get(0);
        assertThat(remainingExpense.getAmount()).isEqualTo(1333 - 1111);

        assertThat(friendshipRepository.findByAddresseeUsernameOrRequesterUsername("ownerUser")).isEmpty();
    }
}