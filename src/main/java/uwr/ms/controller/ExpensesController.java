package uwr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.constant.Currency;
import uwr.ms.constant.Message;
import uwr.ms.model.entity.*;
import uwr.ms.service.ExpensesService;
import uwr.ms.service.TripService;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/trips")
public class ExpensesController {
    private final TripService tripService;
    private final ExpensesService expensesService;

    @Autowired
    public ExpensesController(TripService tripService, ExpensesService expensesService) {
        this.tripService = tripService;
        this.expensesService = expensesService;
    }

    @GetMapping("/expenses/{id}")
    public String getExpenses(@PathVariable("id") Long tripId, Model model, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripEntity trip;
        try {
            trip = tripService.findTripById(tripId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(Message.INVALID_TRIP_ID.toString(), tripId)));
            boolean isParticipant = trip.getParticipants().stream()
                    .anyMatch(participant -> participant.getUser().getUsername().equals(username));
            if (!isParticipant) {
                throw new AccessDeniedException(Message.TRIP_NOT_PARTICIPANT.toString());
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
            return "trips/my_trips";
        }

        List<UserEntity> tripParticipants = tripService.findAllParticipantsByTripId(tripId)
                .stream()
                .map(TripParticipantEntity::getUser)
                .sorted(Comparator.comparing(UserEntity::getName))
                .collect(Collectors.toList());

        List<ExpenseEntity> expenses = expensesService.findAllExpensesByTripId(tripId);
        List<BalanceEntity> balances = expensesService.findAllBalancesByTripId(tripId);

        Map<String, Integer> netBalances = new HashMap<>();
        for (UserEntity participant : tripParticipants) {
            netBalances.put(participant.getUsername(), 0);
        }
        for (BalanceEntity balance : balances) {
            netBalances.put(balance.getDebtor().getUsername(), netBalances.get(balance.getDebtor().getUsername()) - balance.getAmount());
            netBalances.put(balance.getCreditor().getUsername(), netBalances.get(balance.getCreditor().getUsername()) + balance.getAmount());
        }
        netBalances.entrySet().removeIf(entry -> entry.getValue() == 0);

        List<UserBalanceDTO> userBalanceDTOs = tripParticipants.stream()
                .filter(participant -> netBalances.containsKey(participant.getUsername()))
                .map(participant -> new UserBalanceDTO(participant.getUsername(), participant.getName(), netBalances.get(participant.getUsername()))).toList();

        int maxBalance = userBalanceDTOs.stream()
                .mapToInt(UserBalanceDTO::balance)
                .map(Math::abs)
                .max()
                .orElse(1);

        model.addAttribute("maxBalance", maxBalance);
        model.addAttribute("expenses", expenses);
        model.addAttribute("trip", trip);
        model.addAttribute("tripParticipants", tripParticipants);
        model.addAttribute("currencies", Arrays.asList(Currency.values()));
        model.addAttribute("balances", balances);
        model.addAttribute("netBalances", userBalanceDTOs);

        return "expenses/expenses";
    }

    @PostMapping("/add-expense/{tripId}")
    public String addExpense(@PathVariable("tripId") Long tripId, @ModelAttribute ExpenseForm expenseForm, RedirectAttributes redirectAttributes, Model model) {
        try {
            expensesService.saveExpense(tripId, expenseForm);
            redirectAttributes.addFlashAttribute("successMessage", Message.EXPENSE_ADDED_SUCCESS.toString());
            return "redirect:/trips/expenses/" + tripId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
            return "redirect:/trips/expenses/" + tripId;
        }
    }

    public record ExpenseForm(String title, Integer amount, String currency, LocalDate date, String payerUsername, List<String> participantUsernames, List<Integer> participantAmounts) {}
    public record UserBalanceDTO(String username, String name, int balance) {}
}
