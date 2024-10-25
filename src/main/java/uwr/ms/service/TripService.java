package uwr.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.EventType;
import uwr.ms.constant.Message;
import uwr.ms.constant.TripParticipantRole;
import uwr.ms.controller.TripController;
import uwr.ms.model.entity.*;
import uwr.ms.model.repository.*;

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
    private final EventEntityRepository eventEntityRepository;
    private final ExpenseEntityRepository expenseEntityRepository;
    private final ExpenseParticipantEntityRepository expenseParticipantEntityRepository;

    @Autowired
    public TripService(TripEntityRepository tripRepository, UserEntityRepository userRepository, TripParticipantEntityRepository tripParticipantEntityRepository, TripInvitationRepository tripInvitationRepository, EventEntityRepository eventEntityRepository, ExpenseEntityRepository expenseEntityRepository, ExpenseParticipantEntityRepository expenseParticipantEntityRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripParticipantEntityRepository = tripParticipantEntityRepository;
        this.tripInvitationRepository = tripInvitationRepository;
        this.eventEntityRepository = eventEntityRepository;
        this.expenseEntityRepository = expenseEntityRepository;
        this.expenseParticipantEntityRepository = expenseParticipantEntityRepository;
    }

    @Transactional
    public void createTrip(TripEntity trip, String ownerUsername) {
        UserEntity owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), ownerUsername)));
        if (tripRepository.existsByNameAndOwnerUsername(trip.getName(), ownerUsername))
            throw new IllegalStateException(Message.TRIP_NAME_EXISTS.toString());
        if(trip.getName().length() > 30)
            throw new IllegalArgumentException(Message.TRIP_NAME_TOO_LONG.toString());
        TripParticipantEntity ownerParticipant = new TripParticipantEntity();
        ownerParticipant.setUser(owner);
        ownerParticipant.setRole(TripParticipantRole.OWNER);
        trip.addParticipant(ownerParticipant);
        tripRepository.save(trip);
    }

    @Transactional(readOnly = true)
    public List<TripController.TripDTO> findAllTripsByUser(String username) {
        List<TripEntity> trips = tripParticipantEntityRepository.findDistinctTripsByUserUsername(username);
        return trips.stream()
                .map(trip -> new TripController.TripDTO(
                        trip.getId(),
                        trip.getName(),
                        trip.getStartDate(),
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
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.INVALID_TRIP_ID.toString(), tripId)));
        return isUserOwner(trip, username);
    }

    @Transactional(readOnly = true)
    public Optional<TripEntity> findTripById(Long id) {
        return tripRepository.findById(id);
    }

    @Transactional
    public void updateTrip(Long tripId, TripEntity updatedTrip) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.INVALID_TRIP_ID.toString(), tripId)));
        if(trip.getName().length() > 30)
            throw new IllegalArgumentException(Message.TRIP_NAME_TOO_LONG.toString());
        trip.setName(updatedTrip.getName());
        trip.setStartDate(updatedTrip.getStartDate());
        trip.setLocation(updatedTrip.getLocation());
        trip.setDescription(updatedTrip.getDescription());
        trip.setDefaultCurrency(updatedTrip.getDefaultCurrency());
        tripRepository.save(trip);
    }

    @Transactional
    public Set<TripParticipantEntity> findAllParticipantsByTripId(Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.INVALID_TRIP_ID.toString(), tripId)));
        return trip.getParticipants();
    }

    @Transactional
    public void sendTripInvitation(Long tripId, String receiverUsername, String senderUsername) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.TRIP_NOT_FOUND.toString()));
        UserEntity invitedUser = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException(Message.USER_TO_INVITE_NOT_FOUND.toString()));
        UserEntity invitingUser = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVITING_USER_NOT_FOUND.toString()));

        if (!isUserOwner(trip, senderUsername)) {
            throw new AccessDeniedException(Message.PERMISSION_DENIED_INVITE.toString());
        }

        boolean isAlreadyParticipant = trip.getParticipants().stream()
                .anyMatch(participant -> participant.getUser().getUsername().equals(receiverUsername));
        if (isAlreadyParticipant) {
            throw new IllegalStateException(Message.USER_ALREADY_PARTICIPANT.toString());
        }

        boolean isAlreadyInvited = tripInvitationRepository.findByTripAndReceiver(trip, invitedUser).isPresent();
        if (isAlreadyInvited) {
            throw new IllegalStateException(Message.INVITATION_ALREADY_SENT.toString());
        }

        TripInvitationEntity invitation = new TripInvitationEntity();
        invitation.setTrip(trip);
        invitation.setReceiver(invitedUser);
        invitation.setSender(invitingUser);
        tripInvitationRepository.save(invitation);
    }

    @Transactional
    public void removeParticipant(Long tripId, String participantUsername, String username) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.TRIP_NOT_FOUND.toString()));
        TripParticipantEntity participant = tripParticipantEntityRepository.findByUserUsername(participantUsername)
                .orElseThrow(() -> new IllegalArgumentException(Message.PARTICIPANT_NOT_FOUND.toString()));

        if (isUserOwner(trip, username) && username.equals(participant.getUser().getUsername()))
            throw new IllegalStateException(Message.OWNER_CANNOT_REMOVE_THEMSELF.toString());
        if(!isUserOwner(trip, username) && !username.equals(participant.getUser().getUsername()))
            throw new AccessDeniedException(Message.PERMISSION_DENIED_REMOVE_PARTICIPANT.toString());
        if (trip.getParticipants().contains(participant)) {
            trip.getParticipants().remove(participant);
            List<ExpenseParticipantEntity> expenseParticipantEntities = expenseParticipantEntityRepository.findAllByParticipantUsernameAndExpenseTripId(participant.getUser().getUsername(), tripId);
            for (ExpenseParticipantEntity expenseParticipant : expenseParticipantEntities) {
                ExpenseEntity expense = expenseParticipant.getExpense();
                expense.setAmount(expense.getAmount() - expenseParticipant.getAmount());
                expense.getExpenseParticipants().remove(expenseParticipant);
            }
            List<ExpenseEntity> expenses = expenseEntityRepository.findAllByPayerUsernameAndTripId(participant.getUser().getUsername(), tripId);
            expenseEntityRepository.deleteAll(expenses);
        } else {
            throw new IllegalStateException(Message.PARTICIPANT_NOT_ASSOCIATED.toString());
        }
    }

    @Transactional(readOnly = true)
    public List<TripInvitationEntity> getTripInvitations(String username) {
        return tripInvitationRepository.findByReceiverUsername(username);
    }

    @Transactional
    public void acceptInvitation(Long invitationId, String username) {
        TripInvitationEntity invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVITATION_NOT_FOUND.toString()));
        TripEntity trip = tripRepository.findById(invitation.getTrip().getId())
                .orElseThrow(() -> new IllegalArgumentException(Message.TRIP_NOT_FOUND.toString()));
        UserEntity user = userRepository.findByUsername(invitation.getReceiver().getUsername())
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), invitation.getReceiver().getUsername())));

        if (!username.equals(user.getUsername()))
            throw new AccessDeniedException(Message.ACCESS_DENIED_TRIP_INVITATION.toString());

        boolean participantExists = tripParticipantEntityRepository.findByTripAndUser(trip, user).isPresent();
        if (participantExists)
            throw new IllegalArgumentException(Message.PARTICIPANT_ALREADY_ADDED.toString());

        TripParticipantEntity participant = new TripParticipantEntity();
        participant.setTrip(trip);
        participant.setUser(user);
        participant.setRole(TripParticipantRole.MEMBER);
        trip.getParticipants().add(participant);
        tripRepository.save(trip);
        tripInvitationRepository.delete(invitation);
    }

    @Transactional
    public void declineInvitation(Long invitationId, String username) {
        TripInvitationEntity invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVITATION_NOT_FOUND.toString()));
        if (!username.equals(invitation.getReceiver().getUsername()))
            throw new AccessDeniedException(Message.ACCESS_DENIED_TRIP_INVITATION.toString());
        tripInvitationRepository.delete(invitation);
    }

    @Transactional
    public void addEventToTrip(EventEntity event, TripEntity trip) {
        if (event.getTime() == null || event.getDate() == null)
            throw new IllegalArgumentException(Message.INVALID_DATE_TIME.toString());
        if (event.getEventType().equals(EventType.SINGLE) && (event.getLocation() == null || event.getLocation().isEmpty()))
            throw new IllegalArgumentException(Message.INVALID_LOCATION.toString());
        if (event.getEventType().equals(EventType.ROUTE) &&
                (event.getOrigin() == null || event.getOrigin().isEmpty() || event.getDestination() == null || event.getDestination().isEmpty()))
            throw new IllegalArgumentException(Message.INVALID_ORIGIN_DESTINATION.toString());
        if (event.getEventType() == EventType.SINGLE) {
            event.setOrigin(null);
            event.setDestination(null);
            event.setTravelMode(null);
        } else if (event.getEventType() == EventType.ROUTE) {
            event.setLocation(null);
        }
        eventEntityRepository.save(event);
        trip.getEvents().add(event);
        event.setTrip(trip);
        tripRepository.save(trip);
    }

    @Transactional
    public void deleteEvent(Long tripId, Long eventId, String username) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.TRIP_NOT_FOUND.toString()));
        if (!isUserOwner(trip, username))
            throw new AccessDeniedException(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
        EventEntity event = eventEntityRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(Message.EVENT_NOT_FOUND.toString()));
        if (!event.getTrip().getId().equals(tripId))
            throw new IllegalArgumentException(Message.EVENT_NOT_BELONG_TO_TRIP.toString());
        trip.getEvents().removeIf(e -> e.getId().equals(eventId));
        eventEntityRepository.delete(event);
    }

    @Transactional
    public void deleteTrip(Long tripId, String username) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.TRIP_NOT_FOUND.toString()));
        if (!isUserOwner(trip, username))
            throw new AccessDeniedException(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
        eventEntityRepository.deleteAllByTripId(tripId);
        tripInvitationRepository.deleteAllByTripId(tripId);
        tripParticipantEntityRepository.deleteAllByTripId(tripId);
        expenseParticipantEntityRepository.deleteAllByExpenseTripId(tripId);
        expenseEntityRepository.deleteAllByTripId(tripId);
        tripRepository.delete(trip);
    }
}

