package uwr.ms.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "trips")
@Data
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TripParticipantEntity> participants = new HashSet<>();

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public TripEntity(String name) {
        this.name = name;
    }

    public void addParticipant(TripParticipantEntity participant) {
        participants.add(participant);
        participant.setTrip(this);
    }

    public TripEntity() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripEntity that = (TripEntity) o;
        return Objects.equals(id, that.id); // Compare only the IDs
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // Hash only the ID
    }
}

