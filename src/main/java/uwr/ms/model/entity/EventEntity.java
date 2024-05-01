package uwr.ms.model.entity;

import jakarta.persistence.Entity;

import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import jakarta.persistence.*;
import uwr.ms.constant.EventType;
import uwr.ms.constant.TravelMode;

@Entity
@Table(name = "events")
@Data
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventName;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String location;
    private String origin;
    private String destination;

    @Min(1)
    @Max(18)
    private int zoom;

    @Enumerated(EnumType.STRING)
    private TravelMode travelMode;

    private LocalDate date;
    private LocalTime time;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private TripEntity trip;
}