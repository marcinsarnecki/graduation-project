package uwr.ms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import uwr.ms.constant.TripParticipantRole;

@Entity
@Data
@Table(name = "trip_participants")
public class TripParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private TripParticipantRole role;
}

