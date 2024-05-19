package uwr.ms.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "expenses")
@Data
public class ExpenseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    String title;

    int amount;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private TripEntity trip;

    @ManyToOne
    @JoinColumn(name = "payer_username")
    private UserEntity payer;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExpenseParticipantEntity> expenseParticipants;

    public void addParticipant(ExpenseParticipantEntity participant) {
        expenseParticipants.add(participant);
        participant.setExpense(this);
    }

    public void removeParticipant(ExpenseParticipantEntity participant) {
        expenseParticipants.remove(participant);
        participant.setExpense(null);
    }


    @Override
    public String toString() {
        return "ExpenseEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", date=" + date.toString() +
                ", tripId=" + trip.getId() +
                ", payer=" + payer.getUsername() +
                '}';
    }
}
