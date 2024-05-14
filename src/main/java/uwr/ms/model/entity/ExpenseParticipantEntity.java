package uwr.ms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expenses_participants")
@Data
@NoArgsConstructor
public class ExpenseParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participant")
    private UserEntity participant;

    int amount;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private ExpenseEntity expense;

    public ExpenseParticipantEntity(UserEntity participant, int amount, ExpenseEntity expense) {
        this.participant = participant;
        this.amount = amount;
        this.expense = expense;
    }
}
