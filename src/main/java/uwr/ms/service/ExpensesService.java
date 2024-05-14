package uwr.ms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.Currency;
import uwr.ms.controller.ExpensesController;
import uwr.ms.model.entity.ExpenseEntity;
import uwr.ms.model.entity.ExpenseParticipantEntity;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ExpensesService {
    private final TripEntityRepository tripRepository;
    private final UserEntityRepository userRepository;
    private final ExpenseEntityRepository expenseRepository;
    private final ExpenseParticipantEntityRepository expenseParticipantRepository;
    private final BalanceEntityRepository balanceRepository;

    public ExpensesService(TripEntityRepository tripRepository, UserEntityRepository userRepository, ExpenseEntityRepository expenseRepository, ExpenseParticipantEntityRepository expenseParticipantRepository, BalanceEntityRepository balanceRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.expenseParticipantRepository = expenseParticipantRepository;
        this.balanceRepository = balanceRepository;
    }

    @Transactional
    public void saveExpense(Long tripId, ExpensesController.ExpenseForm expenseForm) {
        TripEntity trip = tripRepository.findById(tripId).orElseThrow(() -> new IllegalArgumentException("Invalid trip Id:" + tripId));
        ExpenseEntity newExpense = new ExpenseEntity();
        newExpense.setTrip(trip);
        newExpense.setTitle(expenseForm.title());
        newExpense.setDate(expenseForm.date());

        int amountInCents = (int) Math.round(expenseForm.amount() * 100);
        newExpense.setAmount(amountInCents);

        UserEntity payer = userRepository.findByUsername(expenseForm.payerUsername())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));
        newExpense.setPayer(payer);

        List<UserEntity> participants = userRepository.findByUsernameIn(expenseForm.participantUsernames());
        participants.sort(Comparator.comparing(UserEntity::getName));

        List<ExpenseParticipantEntity> expenseParticipants = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            UserEntity participant = participants.get(i);
            int participantAmount = expenseForm.participantAmounts().get(i);
            expenseParticipants.add(new ExpenseParticipantEntity(participant, participantAmount, newExpense));
        }

        newExpense.setExpenseParticipants(expenseParticipants);
        expenseRepository.save(newExpense);
    }

    public List<ExpenseEntity> findAllExpensesByTripId(Long tripId) {
        return expenseRepository.findAllByTripId(tripId);
    }
}
