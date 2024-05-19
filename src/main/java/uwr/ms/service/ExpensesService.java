package uwr.ms.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.Message;
import uwr.ms.controller.ExpensesController;
import uwr.ms.model.entity.*;
import uwr.ms.model.repository.*;

import java.util.*;

@Service
public class ExpensesService {
    private final TripEntityRepository tripRepository;
    private final UserEntityRepository userRepository;
    private final ExpenseEntityRepository expenseRepository;

    public ExpensesService(TripEntityRepository tripRepository, UserEntityRepository userRepository, ExpenseEntityRepository expenseRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<ExpenseEntity> findAllExpensesByTripId(Long tripId) {
        List<ExpenseEntity> expenses = expenseRepository.findAllByTripId(tripId);
        expenses.sort(Comparator.comparing(ExpenseEntity::getDate));
        return expenses;
    }

    @Transactional
    public void saveExpense(Long tripId, ExpensesController.ExpenseForm expenseForm) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.INVALID_TRIP_ID.toString(), tripId)));
        ExpenseEntity newExpense = new ExpenseEntity();
        newExpense.setTrip(trip);
        newExpense.setTitle(expenseForm.title());
        newExpense.setDate(expenseForm.date());

        int amountInCents = expenseForm.amount();
        int summedParticipantAmounts = 0;
        for (int amount : expenseForm.participantAmounts())
            summedParticipantAmounts += amount;
        if (amountInCents != summedParticipantAmounts)
            amountInCents = summedParticipantAmounts;
        newExpense.setAmount(amountInCents);

        UserEntity payer = userRepository.findByUsername(expenseForm.payerUsername())
                .orElseThrow(() -> new IllegalArgumentException(Message.PAYER_NOT_FOUND.toString()));
        newExpense.setPayer(payer);

        List<UserEntity> participants = expenseForm.participantUsernames().stream()
                .map(username -> userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), username)))).toList();

        List<ExpenseParticipantEntity> expenseParticipants = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            UserEntity participant = participants.get(i);
            int participantAmount = expenseForm.participantAmounts().get(i);
            expenseParticipants.add(new ExpenseParticipantEntity(participant, participantAmount, newExpense));
        }

        newExpense.setExpenseParticipants(expenseParticipants);
        expenseRepository.save(newExpense);
    }

    public Map<String, Integer> getNetBalanceMap(List<ExpenseEntity> expenses, List<UserEntity> tripParticipants) {
        Map<String, Integer> balances = new HashMap<>();
        for (UserEntity participant : tripParticipants) {
            balances.put(participant.getUsername(), 0);
        }
        for (ExpenseEntity expense : expenses) {
            balances.put(expense.getPayer().getUsername(), balances.get(expense.getPayer().getUsername()) + expense.getAmount());
            for (ExpenseParticipantEntity participant : expense.getExpenseParticipants()) {
                balances.put(participant.getParticipant().getUsername(), balances.get(participant.getParticipant().getUsername()) - participant.getAmount());
            }
        }
        balances.entrySet().removeIf(entry -> entry.getValue() == 0);
        return balances;
    }

    public List<ExpensesController.DebtDto> getDebtDtoList(Map<String, Integer> balances) {
        PriorityQueue<Pair> creditors = new PriorityQueue<>((a, b) -> b.getAmount() - a.getAmount());
        PriorityQueue<Pair> debtors = new PriorityQueue<>((a, b) -> b.getAmount() - a.getAmount());

        for (Map.Entry<String, Integer> entry : balances.entrySet()) {
            if (entry.getValue() > 0) {
                creditors.offer(new Pair(entry.getKey(), entry.getValue()));
            } else if (entry.getValue() < 0) {
                debtors.offer(new Pair(entry.getKey(), -entry.getValue()));
            }
        }

        List<ExpensesController.DebtDto> debtDtoList = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Pair creditor = creditors.poll();
            Pair debtor = debtors.poll();

            int transferAmount = Math.min(creditor.getAmount(), debtor.getAmount());

            ExpensesController.DebtDto debtDto = new ExpensesController.DebtDto(debtor.getUsername(), creditor.getUsername(), transferAmount);
            debtDtoList.add(debtDto);

            if (creditor.getAmount() > debtor.getAmount()) {
                creditors.offer(new Pair(creditor.getUsername(), creditor.getAmount() - transferAmount));
            } else if (debtor.getAmount() > creditor.getAmount()) {
                debtors.offer(new Pair(debtor.getUsername(), debtor.getAmount() - transferAmount));
            }
        }
        return debtDtoList;
    }

    public void deleteExpense(Long expenseId) {
        expenseRepository.deleteById(expenseId);
    }

    @Data
    @AllArgsConstructor
    private class Pair {
        private String username;
        private int amount;
    }
}
