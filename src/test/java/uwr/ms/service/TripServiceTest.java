package uwr.ms.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.EventType;
import uwr.ms.constant.LoginProvider;
import uwr.ms.constant.Message;
import uwr.ms.controller.ExpensesController;
import uwr.ms.model.entity.*;
import uwr.ms.model.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@Transactional
@ActiveProfiles("postgres")
public class TripServiceTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private ExpensesService expensesService;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private TripEntityRepository tripRepository;

    @Autowired
    private TripParticipantEntityRepository tripParticipantRepository;

    @Autowired
    private TripInvitationRepository tripInvitationRepository;

    @Autowired
    private EventEntityRepository eventEntityRepository;

    @Autowired
    private ExpenseEntityRepository expenseEntityRepository;

    @Autowired
    private ExpenseParticipantEntityRepository expenseParticipantEntityRepository;

    private UserEntity owner, user1, user2;

    @BeforeEach
    void setup() {
        owner = new UserEntity("ownerUser", "Password1", "owner@example.com", "Owner User", LoginProvider.APP, "");
        user1 = new UserEntity("user1", "Password2", "user1@example.com", "User One", LoginProvider.APP, "");
        user2 = new UserEntity("user2", "Password3", "user2@example.com", "User Two", LoginProvider.APP, "");
        userRepository.saveAll(Arrays.asList(owner, user1, user2));
    }

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

    private TripEntity setupTripWithEvent() {
        TripEntity trip = new TripEntity();
        trip.setName("Diving Trip");
        tripService.createTrip(trip, owner.getUsername());

        EventEntity event = createEvent("New Year Party", "Warsaw", LocalDate.of(2024, 1, 1), LocalTime.of(23, 59));
        tripService.addEventToTrip(event, trip);
        return trip;
    }

    @Test
    void createTripSuccessfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Exciting Trip");
        tripService.createTrip(trip, owner.getUsername());

        TripEntity savedTrip = tripParticipantRepository.findDistinctTripsByUserUsername(owner.getUsername()).get(0);
        assertThat(savedTrip).isNotNull();
        assertThat(savedTrip.getName()).isEqualTo("Exciting Trip");
        assertThat(savedTrip.getParticipants()).hasSize(1);
    }

    @Test
    void updateTripDetailsSuccessfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Fun Trip");
        tripService.createTrip(trip, owner.getUsername());

        TripEntity fetchedTrip = tripParticipantRepository.findDistinctTripsByUserUsername(owner.getUsername()).get(0);
        fetchedTrip.setName("Updated Fun Trip");
        tripService.updateTrip(fetchedTrip.getId(), fetchedTrip);

        TripEntity updatedTrip = tripRepository.findById(fetchedTrip.getId()).orElseThrow();
        assertThat(updatedTrip.getName()).isEqualTo("Updated Fun Trip");
    }

    @Test
    void sendTripInvitationSuccessful() {
        TripEntity trip = new TripEntity();
        trip.setName("Adventure Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        assertThat(tripParticipantRepository.findByTripAndUser(trip, user1).isPresent()).isFalse();
        assertThat(tripInvitationRepository.findByTripAndReceiver(trip, user1).isPresent()).isTrue();
    }

    @Test
    void acceptingTripInvitationAddsParticipant() {
        TripEntity trip = new TripEntity();
        trip.setName("Holiday Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId = tripService.getTripInvitations(user1.getUsername()).get(0).getId();

        tripService.acceptInvitation(invitationId, user1.getUsername());

        boolean isParticipant = tripParticipantRepository.findByTripAndUser(trip, user1).isPresent();
        assertThat(isParticipant).isTrue();
    }

    @Test
    void declineTripInvitationRemovesInvitation() {
        TripEntity trip = new TripEntity();
        trip.setName("Explore Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId = tripService.getTripInvitations(user1.getUsername()).get(0).getId();

        tripService.declineInvitation(invitationId, user1.getUsername());

        assertThat(tripService.getTripInvitations(user1.getUsername())).isEmpty();
    }

    @Test
    void removeParticipant_Successfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Camping Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user1.getUsername());

        Set<TripParticipantEntity> initialParticipants = tripService.findAllParticipantsByTripId(trip.getId());
        assertThat(initialParticipants).hasSize(2);

        TripParticipantEntity participantToRemove = initialParticipants.stream()
                .filter(participant -> participant.getUser().getUsername().equals(user1.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User1 should be a participant"));

        tripService.removeParticipant(trip.getId(), participantToRemove.getUser().getUsername(), owner.getUsername());

        Set<TripParticipantEntity> updatedParticipants = tripService.findAllParticipantsByTripId(trip.getId());
        assertThat(updatedParticipants).hasSize(1);
        assertThat(updatedParticipants.stream().anyMatch(part -> part.getUser().getUsername().equals(user1.getUsername()))).isFalse();
    }

    @Test
    void removeParticipantThrowsAccessDeniedException() {
        TripEntity trip = new TripEntity();
        trip.setName("Diving Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user1.getUsername());

        TripParticipantEntity participant = tripService.findAllParticipantsByTripId(trip.getId()).stream()
                .filter(p -> p.getUser().getUsername().equals(user1.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User1 should be a participant"));

        assertThatThrownBy(() -> {
            tripService.removeParticipant(trip.getId(), participant.getUser().getUsername(), user2.getUsername());
        }).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Message.PERMISSION_DENIED_REMOVE_PARTICIPANT.toString());
    }

    @Test
    void addSingleLocationEventSuccessfully() {
        TripEntity trip = new TripEntity();
        trip.setId(1L);
        trip.setName("New Year Trip");
        tripService.createTrip(trip, owner.getUsername());

        EventEntity event = new EventEntity();
        event.setId(1L);
        event.setEventName("New Year Party");
        event.setEventType(EventType.SINGLE);
        event.setLocation("Warsaw");
        event.setZoom(10);
        event.setDate(LocalDate.of(2024, 1, 1));
        event.setTime(LocalTime.of(23, 59));

        tripService.addEventToTrip(event, trip);

        assertThat(trip.getEvents()).hasSize(1);
        assertThat(trip.getEvents().iterator().next().getLocation()).isEqualTo("Warsaw");
        assertThat(trip.getEvents().iterator().next().getOrigin()).isNull();
    }

    @Test
    public void deleteEventSuccessfully() {
        TripEntity trip = setupTripWithEvent();
        EventEntity event = trip.getEvents().iterator().next();
        tripService.deleteEvent(trip.getId(), event.getId(), owner.getUsername());
        Assertions.assertTrue(eventEntityRepository.findById(event.getId()).isEmpty());
    }
    @Test
    public void deleteNonExistingEventThrowsIllegalStateException() {
        TripEntity trip = setupTripWithEvent();
        assertThatThrownBy(() -> {
            tripService.deleteEvent(trip.getId(), 999999L, owner.getUsername());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Message.EVENT_NOT_FOUND.toString());
    }

    @Test
    public void deleteEventInNonExistingTripThrowsIllegalStateException() {
        TripEntity trip = setupTripWithEvent();
        assertThatThrownBy(() -> {
            tripService.deleteEvent(trip.getId() + 1, 1L, owner.getUsername());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Message.TRIP_NOT_FOUND.toString());
    }

    @Test
    public void deleteEventByNonOwnerThrowsAccessDeniedException() {
        TripEntity trip = setupTripWithEvent();
        assertThatThrownBy(() -> {
            tripService.deleteEvent(trip.getId(), 1L, user1.getUsername());
        }).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
    }

    @Test
    public void creatingTripWithTheSameNameThrowsIllegalStateException() {
        TripEntity trip1 = setupTripWithEvent();
        assertThatThrownBy(() -> {
            TripEntity trip2 = setupTripWithEvent();
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Message.TRIP_NAME_EXISTS.toString());
    }

    @Test
    void addAndDeleteMultipleEventsSuccessfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Festival Trip");
        tripService.createTrip(trip, owner.getUsername());

        EventEntity event1 = createEvent("Event 1", "Cracow", LocalDate.of(2023, 10, 10), LocalTime.of(10, 0));
        EventEntity event2 = createEvent("Event 2", "Cracow", LocalDate.of(2023, 10, 11), LocalTime.of(15, 0));
        EventEntity event3 = createEvent("Event 3", "Cracow", LocalDate.of(2023, 10, 12), LocalTime.of(20, 0));
        EventEntity event4 = createEvent("Event 4", "Cracow", LocalDate.of(2023, 10, 13), LocalTime.of(18, 0));

        tripService.addEventToTrip(event1, trip);
        tripService.addEventToTrip(event2, trip);
        tripService.addEventToTrip(event3, trip);
        tripService.addEventToTrip(event4, trip);

        trip = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(trip.getEvents()).hasSize(4);

        tripService.deleteEvent(trip.getId(), event2.getId(), owner.getUsername());
        tripService.deleteEvent(trip.getId(), event4.getId(), owner.getUsername());

        trip = tripRepository.findById(trip.getId()).orElseThrow();
        List<String> remainingEventNames = trip.getEvents().stream().map(EventEntity::getEventName).collect(Collectors.toList());

        assertThat(remainingEventNames).containsExactlyInAnyOrder("Event 1", "Event 3");
        assertThat(remainingEventNames).doesNotContain("Event 2", "Event 4");
    }

    @Test
    void deleteTripSuccessfully() {
        TripEntity trip1 = new TripEntity();
        trip1.setName("Test Trip 1");
        tripService.createTrip(trip1, owner.getUsername());

        tripService.sendTripInvitation(trip1.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        tripService.sendTripInvitation(trip1.getId(), user2.getUsername(), owner.getUsername());
        Long invitationId2 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId2, user2.getUsername());

        UserEntity user3 = new UserEntity("user3", "Password4", "user3@example.com", "User Three", LoginProvider.APP, "");
        userRepository.save(user3);
        tripService.sendTripInvitation(trip1.getId(), user3.getUsername(), owner.getUsername());

        EventEntity event1 = createEvent("Event 1", "Location 1", LocalDate.now(), LocalTime.now());
        EventEntity event2 = createEvent("Event 2", "Location 2", LocalDate.now(), LocalTime.now());
        tripService.addEventToTrip(event1, trip1);
        tripService.addEventToTrip(event2, trip1);

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
                user2.getUsername(),
                Arrays.asList(owner.getUsername(), user1.getUsername()),
                Arrays.asList(1425, 575));
        expensesService.saveExpense(trip1.getId(), expenseForm1);
        expensesService.saveExpense(trip1.getId(), expenseForm2);

        TripEntity trip2 = new TripEntity();
        trip2.setName("Test Trip 2");
        tripService.createTrip(trip2, owner.getUsername());

        tripService.sendTripInvitation(trip2.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId3 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId3, user1.getUsername());

        tripService.sendTripInvitation(trip2.getId(), user2.getUsername(), owner.getUsername());
        Long invitationId4 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId4, user2.getUsername());

        EventEntity event3 = createEvent("Event 3", "Location 3", LocalDate.now(), LocalTime.now());
        EventEntity event4 = createEvent("Event 4", "Location 4", LocalDate.now(), LocalTime.now());
        tripService.addEventToTrip(event3, trip2);
        tripService.addEventToTrip(event4, trip2);

        ExpensesController.ExpenseForm expenseForm3 = new ExpensesController.ExpenseForm(
                "Expense 3",
                1500,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername()),
                Arrays.asList(750, 750));
        ExpensesController.ExpenseForm expenseForm4 = new ExpensesController.ExpenseForm(
                "Expense 4",
                2500,
                LocalDate.now(),
                user2.getUsername(),
                Arrays.asList(owner.getUsername(), user1.getUsername()),
                Arrays.asList(1250, 1250));
        expensesService.saveExpense(trip2.getId(), expenseForm3);
        expensesService.saveExpense(trip2.getId(), expenseForm4);

        assertThat(tripRepository.findById(trip1.getId())).isPresent();
        assertThat(tripRepository.findById(trip2.getId())).isPresent();
        assertThat(eventEntityRepository.findAllByTripId(trip1.getId())).isNotEmpty();
        assertThat(eventEntityRepository.findAllByTripId(trip2.getId())).isNotEmpty();
        assertThat(expenseEntityRepository.findAllByTripId(trip1.getId())).isNotEmpty();
        assertThat(expenseEntityRepository.findAllByTripId(trip2.getId())).isNotEmpty();
        assertThat(tripInvitationRepository.findByTripId(trip1.getId())).isNotEmpty();
        assertThat(tripInvitationRepository.findByTripId(trip2.getId())).isEmpty();

        tripService.deleteTrip(trip1.getId(), owner.getUsername());

        assertThat(tripRepository.findById(trip1.getId())).isEmpty();
        assertThat(eventEntityRepository.findAllByTripId(trip1.getId())).isEmpty();
        assertThat(expenseEntityRepository.findAllByTripId(trip1.getId())).isEmpty();
        assertThat(expenseParticipantEntityRepository.findAllByExpenseTripId(trip1.getId())).isEmpty();
        assertThat(tripInvitationRepository.findByTripId(trip1.getId())).isEmpty();
        assertThat(tripParticipantRepository.findAllByTripId(trip1.getId())).isEmpty();

        assertThat(tripRepository.findById(trip2.getId())).isPresent();
        assertThat(eventEntityRepository.findAllByTripId(trip2.getId())).isNotEmpty();
        assertThat(expenseEntityRepository.findAllByTripId(trip2.getId())).isNotEmpty();
        assertThat(expenseParticipantEntityRepository.findAllByExpenseTripId(trip2.getId())).isNotEmpty();
        assertThat(tripInvitationRepository.findByTripId(trip2.getId())).isEmpty();
        assertThat(tripParticipantRepository.findAllByTripId(trip2.getId())).isNotEmpty();
    }

    @Test
    void removeParticipantSuccessfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Test Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        tripService.sendTripInvitation(trip.getId(), user2.getUsername(), owner.getUsername());
        Long invitationId2 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId2, user2.getUsername());

        ExpensesController.ExpenseForm expenseForm = new ExpensesController.ExpenseForm(
                "Expense 1",
                1000,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername()),
                Arrays.asList(500, 500));
        expensesService.saveExpense(trip.getId(), expenseForm);

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user1)).isPresent();
        assertThat(expenseParticipantEntityRepository.findAllByParticipantUsernameAndExpenseTripId(user1.getUsername(), trip.getId())).isNotEmpty();
        assertThat(expenseEntityRepository.findAllByPayerUsernameAndTripId(user1.getUsername(), trip.getId())).isEmpty();

        tripService.removeParticipant(trip.getId(), user1.getUsername(), owner.getUsername());

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user1)).isEmpty();
        assertThat(expenseParticipantEntityRepository.findAllByParticipantUsernameAndExpenseTripId(user1.getUsername(), trip.getId())).isEmpty();
        assertThat(expenseEntityRepository.findAllByPayerUsernameAndTripId(user1.getUsername(), trip.getId())).isEmpty();

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user2)).isPresent();
    }

    @Test
    void removeParticipantThrowsAccessDeniedExceptionForNonOwner() {
        TripEntity trip = new TripEntity();
        trip.setName("Test Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        tripService.sendTripInvitation(trip.getId(), user2.getUsername(), owner.getUsername());
        Long invitationId2 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId2, user2.getUsername());

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user1)).isPresent();

        assertThatThrownBy(() -> tripService.removeParticipant(trip.getId(), user1.getUsername(), user2.getUsername()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(Message.PERMISSION_DENIED_REMOVE_PARTICIPANT.toString());
    }

    @Test
    void removeParticipantThrowsIllegalStateExceptionForOwnerRemovingThemself() {
        TripEntity trip = new TripEntity();
        trip.setName("Test Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        assertThat(tripParticipantRepository.findByTripAndUser(trip, owner)).isPresent();

        assertThatThrownBy(() -> tripService.removeParticipant(trip.getId(), owner.getUsername(), owner.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Message.OWNER_CANNOT_REMOVE_THEMSELF.toString());
    }

    @Test
    void removeParticipantThrowsIllegalArgumentExceptionForParticipantNotAssociated() {
        TripEntity trip = new TripEntity();
        trip.setName("Test Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user1)).isPresent();

        assertThatThrownBy(() -> tripService.removeParticipant(trip.getId(), "nonExistingUser", owner.getUsername()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Message.PARTICIPANT_NOT_FOUND.toString());
    }
    @Test
    void removeParticipantAndExpensesSuccessfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Test Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId1 = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId1, user1.getUsername());

        tripService.sendTripInvitation(trip.getId(), user2.getUsername(), owner.getUsername());
        Long invitationId2 = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId2, user2.getUsername());

        ExpensesController.ExpenseForm expenseForm1 = new ExpensesController.ExpenseForm(
                "Expense 1",
                1000,
                LocalDate.now(),
                user1.getUsername(),
                Arrays.asList(user2.getUsername(), owner.getUsername()),
                Arrays.asList(500, 500));
        ExpensesController.ExpenseForm expenseForm2 = new ExpensesController.ExpenseForm(
                "Expense 2",
                2000,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername()),
                Arrays.asList(900, 1100));
        expensesService.saveExpense(trip.getId(), expenseForm1);
        expensesService.saveExpense(trip.getId(), expenseForm2);

        assertThat(expenseEntityRepository.findAllByPayerUsernameAndTripId(user1.getUsername(), trip.getId())).hasSize(1);
        assertThat(expenseParticipantEntityRepository.findAllByParticipantUsernameAndExpenseTripId(user1.getUsername(), trip.getId())).hasSize(1);

        tripService.removeParticipant(trip.getId(), user1.getUsername(), owner.getUsername());

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user1)).isEmpty();
        assertThat(expenseEntityRepository.findAllByPayerUsernameAndTripId(user1.getUsername(), trip.getId())).isEmpty();
        assertThat(expenseParticipantEntityRepository.findAllByParticipantUsernameAndExpenseTripId(user1.getUsername(), trip.getId())).isEmpty();

        ExpenseEntity remainingExpense = expenseEntityRepository.findAllByTripId(trip.getId()).get(0);
        assertThat(remainingExpense.getAmount()).isEqualTo(2000 - 900);

        assertThat(tripParticipantRepository.findByTripAndUser(trip, user2)).isPresent();
        assertThat(expenseParticipantEntityRepository.findAllByParticipantUsernameAndExpenseTripId(user2.getUsername(), trip.getId())).hasSize(1);
    }
}