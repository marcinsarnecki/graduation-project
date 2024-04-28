package uwr.ms.service;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.LoginProvider;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.TripEntityRepository;
import uwr.ms.model.repository.TripInvitationRepository;
import uwr.ms.model.repository.TripParticipantEntityRepository;
import uwr.ms.model.repository.UserEntityRepository;

import java.util.Arrays;
import java.util.Set;

@SpringBootTest
@Transactional
@ActiveProfiles("postgres")
public class TripServiceTest {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TripService tripService;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private TripEntityRepository tripRepository;

    @Autowired
    private TripParticipantEntityRepository tripParticipantRepository;

    @Autowired
    private TripInvitationRepository tripInvitationRepository;

    private UserEntity owner, user1, user2;

    @BeforeEach
    void setup() {
        owner = new UserEntity("ownerUser", "Password1", "owner@example.com", "Owner User", LoginProvider.APP, "");
        user1 = new UserEntity("user1", "Password2", "user1@example.com", "User One", LoginProvider.APP, "");
        user2 = new UserEntity("user2", "Password3", "user2@example.com", "User Two", LoginProvider.APP, "");
        userRepository.saveAll(Arrays.asList(owner, user1, user2));
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

        tripService.acceptInvitation(invitationId);

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

        tripService.declineInvitation(invitationId);

        assertThat(tripService.getTripInvitations(user1.getUsername())).isEmpty();
    }

    @Test
    void removeParticipant_Successfully() {
        TripEntity trip = new TripEntity();
        trip.setName("Camping Trip");
        tripService.createTrip(trip, owner.getUsername());

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        Long invitationId = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId);

        Set<TripParticipantEntity> initialParticipants = tripService.findAllParticipantsByTripId(trip.getId());
        assertThat(initialParticipants).hasSize(2);

        TripParticipantEntity participantToRemove = initialParticipants.stream()
                .filter(participant -> participant.getUser().getUsername().equals(user1.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User1 should be a participant"));

        tripService.removeParticipant(trip.getId(), participantToRemove.getId(), owner.getUsername());

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
        tripService.acceptInvitation(invitationId);

        TripParticipantEntity participant = tripService.findAllParticipantsByTripId(trip.getId()).stream()
                .filter(p -> p.getUser().getUsername().equals(user1.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User1 should be a participant"));

        assertThatThrownBy(() -> {
            tripService.removeParticipant(trip.getId(), participant.getId(), user1.getUsername());
        }).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to remove participants from this trip");
    }

}