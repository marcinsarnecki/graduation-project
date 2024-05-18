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
    private final TripParticipantEntityRepository tripParticipantRepository;
    private final BalanceEntityRepository balanceRepository;

    public ExpensesService(TripEntityRepository tripRepository, UserEntityRepository userRepository, ExpenseEntityRepository expenseRepository, TripParticipantEntityRepository tripParticipantRepository, BalanceEntityRepository balanceRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.tripParticipantRepository = tripParticipantRepository;
        this.balanceRepository = balanceRepository;
    }

    public List<ExpenseEntity> findAllExpensesByTripId(Long tripId) {
        List<ExpenseEntity> expenses = expenseRepository.findAllByTripId(tripId);
        expenses.sort(Comparator.comparing(ExpenseEntity::getDate));
        return expenses;
    }

    public List<BalanceEntity> findAllBalancesByTripId(Long tripId) {
        return balanceRepository.findAllByTripId(tripId);
    }

    @Transactional
    public void saveExpense(Long tripId, ExpensesController.ExpenseForm expenseForm) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVALID_TRIP_ID + String.valueOf(tripId)));
        ExpenseEntity newExpense = new ExpenseEntity();
        newExpense.setTrip(trip);
        newExpense.setTitle(expenseForm.title());
        newExpense.setDate(expenseForm.date());

        int amountInCents = expenseForm.amount();
        int summedParticipantAmounts = 0;
        for(int amount: expenseForm.participantAmounts())
            summedParticipantAmounts += amount;
        if(amountInCents != summedParticipantAmounts)
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
        updateBalances(trip, newExpense);
        expenseRepository.save(newExpense);
    }

    private void updateBalances(TripEntity trip, ExpenseEntity newExpense) {
        List<BalanceEntity> balanceEntities = balanceRepository.findAllByTripId(trip.getId());
        balanceRepository.deleteAllByTripId(trip.getId());
        List<String> usernames = tripParticipantRepository.findByTripId(trip.getId()).stream().map(TripParticipantEntity::getUser).map(UserEntity::getUsername).toList();
        Map<String, Integer> balances = new HashMap<>();
        for (String username : usernames) {
            balances.put(username, 0);
        }
        for (BalanceEntity balance : balanceEntities) {
            balances.put(balance.getDebtor().getUsername(), balances.get(balance.getDebtor().getUsername()) - balance.getAmount());
            balances.put(balance.getCreditor().getUsername(), balances.get(balance.getCreditor().getUsername()) + balance.getAmount());
        }
        balances.put(newExpense.getPayer().getUsername(), balances.get(newExpense.getPayer().getUsername()) + newExpense.getAmount());
        for (ExpenseParticipantEntity participant : newExpense.getExpenseParticipants()) {
            balances.put(participant.getParticipant().getUsername(), balances.get(participant.getParticipant().getUsername()) - participant.getAmount());
        }
        PriorityQueue<Pair> creditors = new PriorityQueue<>((a, b) -> b.getAmount() - a.getAmount());
        PriorityQueue<Pair> debtors = new PriorityQueue<>((a, b) -> b.getAmount() - a.getAmount());

        for (Map.Entry<String, Integer> entry : balances.entrySet()) {
            if (entry.getValue() > 0) {
                creditors.offer(new Pair(entry.getKey(), entry.getValue()));
            } else if (entry.getValue() < 0) {
                debtors.offer(new Pair(entry.getKey(), -entry.getValue()));
            }
        }
        List<BalanceEntity> newBalances = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Pair creditor = creditors.poll();
            Pair debtor = debtors.poll();

            int transferAmount = Math.min(creditor.getAmount(), debtor.getAmount());

            BalanceEntity balance = new BalanceEntity();
            balance.setTrip(trip);
            balance.setDebtor(userRepository.findByUsername(debtor.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), debtor.getUsername()))));
            balance.setCreditor(userRepository.findByUsername(creditor.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), creditor.getUsername()))));
            balance.setAmount(transferAmount);
            newBalances.add(balance);

            if (creditor.getAmount() > debtor.getAmount()) {
                creditors.offer(new Pair(creditor.getUsername(), creditor.getAmount() - transferAmount));
            } else if (debtor.getAmount() > creditor.getAmount()) {
                debtors.offer(new Pair(debtor.getUsername(), debtor.getAmount() - transferAmount));
            }
        }
        balanceRepository.saveAll(newBalances);
    }

    @Data
    @AllArgsConstructor
    private class Pair {
        private String username;
        private int amount;
    }
}
