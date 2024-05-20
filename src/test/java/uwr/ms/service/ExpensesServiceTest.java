package uwr.ms.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.Currency;
import uwr.ms.constant.LoginProvider;
import uwr.ms.controller.ExpensesController;
import uwr.ms.model.entity.ExpenseEntity;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@Transactional
@ActiveProfiles("postgres")
public class ExpensesServiceTest {

    @Autowired
    private ExpensesService expensesService;

    @Autowired
    private TripService tripService;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private ExpenseEntityRepository expenseRepository;

    private UserEntity owner, user1, user2, user3, user4, user5, user6;

    @BeforeEach
    void setup() {
        owner = new UserEntity("ownerUser", "Password1", "owner@example.com", "Owner User", LoginProvider.APP, "");
        user1 = new UserEntity("user1", "Password2", "user1@example.com", "User One", LoginProvider.APP, "");
        user2 = new UserEntity("user2", "Password3", "user2@example.com", "User Two", LoginProvider.APP, "");
        user3 = new UserEntity("user3", "Password4", "user3@example.com", "User Three", LoginProvider.APP, "");
        user4 = new UserEntity("user4", "Password5", "user4@example.com", "User Four", LoginProvider.APP, "");
        user5 = new UserEntity("user5", "Password6", "user5@example.com", "User Five", LoginProvider.APP, "");
        user6 = new UserEntity("user6", "Password7", "user6@example.com", "User Six", LoginProvider.APP, "");
        userRepository.saveAll(Arrays.asList(owner, user1, user2, user3, user4, user5, user6));
    }

    private TripEntity setupTrip() {
        TripEntity trip = new TripEntity();
        trip.setName("Diving Trip");
        trip.setDefaultCurrency(Currency.USD);
        tripService.createTrip(trip, owner.getUsername());
        Long invitationId;

        tripService.sendTripInvitation(trip.getId(), user1.getUsername(), owner.getUsername());
        invitationId = tripService.getTripInvitations(user1.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user1.getUsername());

        tripService.sendTripInvitation(trip.getId(), user2.getUsername(), owner.getUsername());
        invitationId = tripService.getTripInvitations(user2.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user2.getUsername());

        tripService.sendTripInvitation(trip.getId(), user3.getUsername(), owner.getUsername());
        invitationId = tripService.getTripInvitations(user3.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user3.getUsername());

        tripService.sendTripInvitation(trip.getId(), user4.getUsername(), owner.getUsername());
        invitationId = tripService.getTripInvitations(user4.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user4.getUsername());

        tripService.sendTripInvitation(trip.getId(), user5.getUsername(), owner.getUsername());
        invitationId = tripService.getTripInvitations(user5.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user5.getUsername());

        tripService.sendTripInvitation(trip.getId(), user6.getUsername(), owner.getUsername());
        invitationId = tripService.getTripInvitations(user6.getUsername()).get(0).getId();
        tripService.acceptInvitation(invitationId, user6.getUsername());
        return trip;
    }

    @Test
    void addExpenseSuccessfully() {
        TripEntity trip = setupTrip();

        ExpensesController.ExpenseForm expenseForm = new ExpensesController.ExpenseForm(
                "Dinner",
                10000,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername(), user3.getUsername()),
                Arrays.asList(2000, 6000, 2000)
        );

        expensesService.saveExpense(trip.getId(), expenseForm);

        List<ExpenseEntity> expenses = expenseRepository.findAllByTripId(trip.getId());
        assertThat(expenses).hasSize(1);

        ExpenseEntity expense = expenses.get(0);
        assertThat(expense.getTitle()).isEqualTo("Dinner");
        assertThat(expense.getAmount()).isEqualTo(10000);
        assertThat(expense.getPayer().getUsername()).isEqualTo(owner.getUsername());
    }

    @Test
    void addMultipleExpensesAndVerifyBalances() {
        TripEntity trip = setupTrip();

        Map<String, Integer> expectedBalances = new HashMap<>();
        expectedBalances.put(owner.getUsername(), 0);
        expectedBalances.put(user1.getUsername(), 0);
        expectedBalances.put(user2.getUsername(), 0);
        expectedBalances.put(user3.getUsername(), 0);
        expectedBalances.put(user4.getUsername(), 0);
        expectedBalances.put(user5.getUsername(), 0);
        expectedBalances.put(user6.getUsername(), 0);

        ExpensesController.ExpenseForm expenseForm1 = new ExpensesController.ExpenseForm(
                "Lunch",
                6000,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername(), user3.getUsername()),
                Arrays.asList(1500, 3000, 1500)
        );
        expensesService.saveExpense(trip.getId(), expenseForm1);

        expectedBalances.put(owner.getUsername(), expectedBalances.get(owner.getUsername()) + 6000);
        expectedBalances.put(user1.getUsername(), expectedBalances.get(user1.getUsername()) - 1500);
        expectedBalances.put(user2.getUsername(), expectedBalances.get(user2.getUsername()) - 3000);
        expectedBalances.put(user3.getUsername(), expectedBalances.get(user3.getUsername()) - 1500);

        ExpensesController.ExpenseForm expenseForm2 = new ExpensesController.ExpenseForm(
                "Taxi",
                4000,
                LocalDate.now(),
                user1.getUsername(),
                Arrays.asList(owner.getUsername(), user2.getUsername(), user3.getUsername()),
                Arrays.asList(1000, 2000, 1000)
        );
        expensesService.saveExpense(trip.getId(), expenseForm2);

        expectedBalances.put(user1.getUsername(), expectedBalances.get(user1.getUsername()) + 4000);
        expectedBalances.put(owner.getUsername(), expectedBalances.get(owner.getUsername()) - 1000);
        expectedBalances.put(user2.getUsername(), expectedBalances.get(user2.getUsername()) - 2000);
        expectedBalances.put(user3.getUsername(), expectedBalances.get(user3.getUsername()) - 1000);

        ExpensesController.ExpenseForm expenseForm3 = new ExpensesController.ExpenseForm(
                "Hotel",
                12000,
                LocalDate.now(),
                user2.getUsername(),
                Arrays.asList(owner.getUsername(), user1.getUsername(), user3.getUsername(), user4.getUsername()),
                Arrays.asList(3000, 3000, 3000, 3000)
        );
        expensesService.saveExpense(trip.getId(), expenseForm3);

        expectedBalances.put(user2.getUsername(), expectedBalances.get(user2.getUsername()) + 12000);
        expectedBalances.put(owner.getUsername(), expectedBalances.get(owner.getUsername()) - 3000);
        expectedBalances.put(user1.getUsername(), expectedBalances.get(user1.getUsername()) - 3000);
        expectedBalances.put(user3.getUsername(), expectedBalances.get(user3.getUsername()) - 3000);
        expectedBalances.put(user4.getUsername(), expectedBalances.get(user4.getUsername()) - 3000);


        List<UserEntity> tripParticipants = tripService.findAllParticipantsByTripId(trip.getId())
                .stream()
                .map(TripParticipantEntity::getUser)
                .collect(Collectors.toList());

        List<ExpenseEntity> expenses = expensesService.findAllExpensesByTripId(trip.getId());
        Map<String, Integer> netBalanceMap = expensesService.getNetBalanceMap(expenses, tripParticipants);
        List<ExpensesController.DebtDto> debtDtoList = expensesService.getDebtDtoList(netBalanceMap);

        assertThat(debtDtoList).isNotEmpty();
        assertThat(debtDtoList.size()).isLessThanOrEqualTo(expectedBalances.size() - 1);

        for (ExpensesController.DebtDto debtDto : debtDtoList) {
            String debtorUsername = debtDto.debtorUsername();
            String creditorUsername = debtDto.creditorUsername();
            int amount = debtDto.amount();

            expectedBalances.put(debtorUsername, expectedBalances.get(debtorUsername) + amount);
            expectedBalances.put(creditorUsername, expectedBalances.get(creditorUsername) - amount);
        }

        for (Map.Entry<String, Integer> entry : expectedBalances.entrySet()) {
            assertThat(entry.getValue()).isEqualTo(0);
        }
    }

    @Test
    void addMultipleExpensesAndVerifyBalances2() {
        TripEntity trip = setupTrip();

        Map<String, Integer> expectedBalances = new HashMap<>();
        expectedBalances.put(owner.getUsername(), 0);
        expectedBalances.put(user1.getUsername(), 0);
        expectedBalances.put(user2.getUsername(), 0);
        expectedBalances.put(user3.getUsername(), 0);
        expectedBalances.put(user4.getUsername(), 0);
        expectedBalances.put(user5.getUsername(), 0);
        expectedBalances.put(user6.getUsername(), 0);

        ExpensesController.ExpenseForm expenseForm1 = new ExpensesController.ExpenseForm(
                "Lunch",
                5000,
                LocalDate.now(),
                owner.getUsername(),
                Arrays.asList(user1.getUsername(), user2.getUsername(), user3.getUsername()),
                Arrays.asList(347, 3597, 1056)
        );
        expensesService.saveExpense(trip.getId(), expenseForm1);

        expectedBalances.put(owner.getUsername(), expectedBalances.get(owner.getUsername()) + 5000);
        expectedBalances.put(user1.getUsername(), expectedBalances.get(user1.getUsername()) - 347);
        expectedBalances.put(user2.getUsername(), expectedBalances.get(user2.getUsername()) - 3597);
        expectedBalances.put(user3.getUsername(), expectedBalances.get(user3.getUsername()) - 1056);

        ExpensesController.ExpenseForm expenseForm2 = new ExpensesController.ExpenseForm(
                "Market",
                4000,
                LocalDate.now(),
                user1.getUsername(),
                Arrays.asList(owner.getUsername(), user2.getUsername(), user3.getUsername()),
                Arrays.asList(417, 3299, 284)
        );
        expensesService.saveExpense(trip.getId(), expenseForm2);

        expectedBalances.put(user1.getUsername(), expectedBalances.get(user1.getUsername()) + 4000);
        expectedBalances.put(owner.getUsername(), expectedBalances.get(owner.getUsername()) - 417);
        expectedBalances.put(user2.getUsername(), expectedBalances.get(user2.getUsername()) - 3299);
        expectedBalances.put(user3.getUsername(), expectedBalances.get(user3.getUsername()) - 284);

        ExpensesController.ExpenseForm expenseForm3 = new ExpensesController.ExpenseForm(
                "Restaurant",
                12000,
                LocalDate.now(),
                user2.getUsername(),
                Arrays.asList(owner.getUsername(), user1.getUsername(), user3.getUsername(), user6.getUsername()),
                Arrays.asList(3454, 4423, 1302, 2821)
        );
        expensesService.saveExpense(trip.getId(), expenseForm3);

        expectedBalances.put(user2.getUsername(), expectedBalances.get(user2.getUsername()) + 12000);
        expectedBalances.put(owner.getUsername(), expectedBalances.get(owner.getUsername()) - 3454);
        expectedBalances.put(user1.getUsername(), expectedBalances.get(user1.getUsername()) - 4423);
        expectedBalances.put(user3.getUsername(), expectedBalances.get(user3.getUsername()) - 1302);
        expectedBalances.put(user6.getUsername(), expectedBalances.get(user6.getUsername()) - 2821);

        List<UserEntity> tripParticipants = tripService.findAllParticipantsByTripId(trip.getId())
                .stream()
                .map(TripParticipantEntity::getUser)
                .collect(Collectors.toList());

        List<ExpenseEntity> expenses = expensesService.findAllExpensesByTripId(trip.getId());
        Map<String, Integer> netBalanceMap = expensesService.getNetBalanceMap(expenses, tripParticipants);
        List<ExpensesController.DebtDto> debtDtoList = expensesService.getDebtDtoList(netBalanceMap);

        assertThat(debtDtoList).isNotEmpty();
        assertThat(debtDtoList.size()).isLessThanOrEqualTo(expectedBalances.size() - 1);

        for (ExpensesController.DebtDto debtDto : debtDtoList) {
            String debtorUsername = debtDto.debtorUsername();
            String creditorUsername = debtDto.creditorUsername();
            int amount = debtDto.amount();

            expectedBalances.put(debtorUsername, expectedBalances.get(debtorUsername) + amount);
            expectedBalances.put(creditorUsername, expectedBalances.get(creditorUsername) - amount);
        }
    }
}

