package uwr.ms.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.TripParticipantRole;
import uwr.ms.dto.TripDTO;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripInvitationEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.TripEntityRepository;
import uwr.ms.model.repository.TripInvitationRepository;
import uwr.ms.model.repository.TripParticipantEntityRepository;
import uwr.ms.model.repository.UserEntityRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TripService {
    private final TripEntityRepository tripRepository;
    private final UserEntityRepository userRepository;
    private final TripParticipantEntityRepository tripParticipantEntityRepository;
    private final TripInvitationRepository tripInvitationRepository;

    @Autowired
    public TripService(TripEntityRepository tripRepository, UserEntityRepository userRepository, TripParticipantEntityRepository tripParticipantEntityRepository, TripInvitationRepository tripInvitationRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripParticipantEntityRepository = tripParticipantEntityRepository;
        this.tripInvitationRepository = tripInvitationRepository;
    }

    @Transactional
    public void createTrip(TripEntity trip, String ownerUsername) {
        UserEntity owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (tripRepository.existsByNameAndOwnerUsername(trip.getName(), ownerUsername)) {
            throw new IllegalStateException("You already have a trip with the same name");
        }
        TripParticipantEntity ownerParticipant = new TripParticipantEntity();
        ownerParticipant.setUser(owner);
        ownerParticipant.setRole(TripParticipantRole.OWNER);
        trip.addParticipant(ownerParticipant);
        tripRepository.save(trip);
    }

    @Transactional(readOnly = true)
    public List<TripDTO> findAllTripsByUser(String username) {
        List<TripEntity> trips = tripParticipantEntityRepository.findDistinctTripsByUserUsername(username);
        return trips.stream()
                .map(trip -> new TripDTO(
                        trip.getId(),
                        trip.getName(),
                        trip.getStartDate(),
                        trip.getLocation(),
                        trip.getDescription(),
                        isUserOwner(trip, username)
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isUserOwner(TripEntity trip, String username) {
        return trip.getParticipants().stream()
                .anyMatch(participant -> participant.getUser().getUsername().equals(username) &&
                        participant.getRole() == TripParticipantRole.OWNER);
    }

    @Transactional(readOnly = true)
    public boolean isUserOwner(Long tripId, String username) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Trip ID: " + tripId));
        return isUserOwner(trip, username);
    }

    @Transactional(readOnly = true)
    public Optional<TripEntity> findTripById(Long id) {
        return tripRepository.findById(id);
    }

    @Transactional
    public void updateTrip(Long id, TripEntity updatedTrip) {
        TripEntity trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid trip Id:" + id));
        trip.setName(updatedTrip.getName());
        trip.setStartDate(updatedTrip.getStartDate());
        trip.setLocation(updatedTrip.getLocation());
        trip.setDescription(updatedTrip.getDescription());
        tripRepository.save(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripParticipantEntity> findParticipantsByTrip(Long tripId, Pageable pageable) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Trip ID: " + tripId));
        return tripParticipantEntityRepository.findByTrip(trip, pageable);
    }

    @Transactional
    public Set<TripParticipantEntity> findAllParticipantsByTripId(Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Trip ID: " + tripId));
        return trip.getParticipants();
    }

    @Transactional
    public void sendTripInvitation(Long tripId, String receiverUsername, String senderUsername) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        UserEntity invitedUser = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("User to invite not found"));
        UserEntity invitingUser = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Inviting user not found"));

        if (!isUserOwner(trip, senderUsername)) {
            throw new AccessDeniedException("You do not have permission to invite participants to this trip");
        }

        boolean isAlreadyParticipant = trip.getParticipants().stream()
                .anyMatch(participant -> participant.getUser().getUsername().equals(receiverUsername));
        if (isAlreadyParticipant) {
            throw new IllegalStateException("User is already a participant of the trip");
        }

        boolean isAlreadyInvited = tripInvitationRepository.findByTripAndReceiver(trip, invitedUser).isPresent();
        if (isAlreadyInvited) {
            throw new IllegalStateException("An invitation has already been sent to this user");
        }

        TripInvitationEntity invitation = new TripInvitationEntity();
        invitation.setTrip(trip);
        invitation.setReceiver(invitedUser);
        invitation.setSender(invitingUser);
        tripInvitationRepository.save(invitation);
    }

    @Transactional
    public void removeParticipant(Long tripId, Long participantId, String username) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        TripParticipantEntity participant = tripParticipantEntityRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (!isUserOwner(trip, username)) {
            throw new AccessDeniedException("You do not have permission to remove participants from this trip");
        }

        if (trip.getParticipants().contains(participant)) {
            trip.getParticipants().remove(participant);
        } else {
            throw new IllegalStateException("Participant is not associated with this trip");
        }
    }

    public List<TripInvitationEntity> getTripInvitations(String username) {
        return tripInvitationRepository.findByReceiverUsername(username);
    }

    @Transactional
    public void acceptInvitation(Long invitationId) {
        TripInvitationEntity invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        TripEntity trip = tripRepository.findById(invitation.getTrip().getId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        UserEntity user = userRepository.findByUsername(invitation.getReceiver().getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean participantExists = tripParticipantEntityRepository.findByTripAndUser(trip, user).isPresent();
        if (participantExists)
            throw new IllegalArgumentException("Participant is already added to this trip");
        TripParticipantEntity participant = new TripParticipantEntity();
        participant.setTrip(trip);
        participant.setUser(user);
        participant.setRole(TripParticipantRole.MEMBER);
        trip.getParticipants().add(participant);
        tripRepository.save(trip);
        tripInvitationRepository.delete(invitation);
    }

    public void declineInvitation(Long invitationId) {
        TripInvitationEntity invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        tripInvitationRepository.delete(invitation);
    }
}

