package uwr.ms.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.TripParticipantRole;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.TripEntityRepository;
import uwr.ms.model.repository.TripParticipantEntityRepository;
import uwr.ms.model.repository.UserEntityRepository;

@Service
public class TripService {
    private final TripEntityRepository tripRepository;
    private final UserEntityRepository userRepository;

    private final TripParticipantEntityRepository tripParticipantEntityRepository;

    @Autowired
    public TripService(TripEntityRepository tripRepository, UserEntityRepository userRepository, TripParticipantEntityRepository tripParticipantEntityRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripParticipantEntityRepository = tripParticipantEntityRepository;
    }

    @Transactional
    public void createTrip(TripEntity trip, String ownerUsername) {
        UserEntity owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (tripRepository.existsByNameAndOwnerUsername(trip.getName(), ownerUsername)) { //todo
            throw new IllegalStateException("You already have a trip with the same name");
        }
        TripParticipantEntity ownerParticipant = new TripParticipantEntity();
        ownerParticipant.setUser(owner);
        ownerParticipant.setRole(TripParticipantRole.OWNER); // Assuming you have this enum defined
        trip.addParticipant(ownerParticipant);
        tripRepository.save(trip);
    }



    @Transactional
    public TripParticipantEntity addParticipant(Long tripId, String username) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean participantExists = tripParticipantEntityRepository.findByTripAndUser(trip, user).isPresent();
        if (participantExists)
            throw new IllegalArgumentException("Participant is already added to this trip");
        TripParticipantEntity participant = new TripParticipantEntity();
        participant.setTrip(trip);
        participant.setUser(user);
        return tripParticipantEntityRepository.save(participant);
    }



    // More methods for updating, deleting trips, managing participants, etc.
}

