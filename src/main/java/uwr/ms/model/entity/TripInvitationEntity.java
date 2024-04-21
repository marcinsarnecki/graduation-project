package uwr.ms.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "trip_invitations")
public class TripInvitationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne
    @JoinColumn(name = "sender_username", nullable = false)
    private UserEntity sender;

    @ManyToOne
    @JoinColumn(name = "receiver_username", nullable = false)
    private UserEntity receiver;
}


