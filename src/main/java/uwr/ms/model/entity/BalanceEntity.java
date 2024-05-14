package uwr.ms.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "balances")
@Data
public class BalanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private TripEntity trip;

    @ManyToOne
    @JoinColumn(name = "debtor_username")
    private UserEntity debtor;

    @ManyToOne
    @JoinColumn(name = "creditor_username")
    private UserEntity creditor;

    private int amount;
}
