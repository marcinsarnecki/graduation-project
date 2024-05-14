package uwr.ms.controller;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.constant.Currency;
import uwr.ms.model.entity.*;
import uwr.ms.model.repository.UserEntityRepository;
import uwr.ms.service.AppUserService;
import uwr.ms.service.ExpensesService;
import uwr.ms.service.FriendshipService;
import uwr.ms.service.TripService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/trips")
public class ExpensesController {
    private final TripService tripService;
    private final FriendshipService friendshipService;
    private final ExpensesService expensesService;

    @Autowired
    public ExpensesController(TripService tripService, FriendshipService friendshipService, ExpensesService expensesService) {
        this.tripService = tripService;
        this.friendshipService = friendshipService;
        this.expensesService = expensesService;
    }

    @GetMapping("/expenses/{id}")
    public String getExpenses(@PathVariable("id") Long tripId, Model model, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripEntity trip;
        try {
            trip = tripService.findTripById(tripId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid trip Id:" + tripId));
            boolean isParticipant = trip.getParticipants().stream()
                    .anyMatch(participant -> participant.getUser().getUsername().equals(username));
            if (!isParticipant) {
                throw new AccessDeniedException("You are not participant of this trip.");
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
        expenses.sort(Comparator.comparing(ExpenseEntity::getDate));

//        expenses.forEach(expense -> expense.setExpenseParticipants(
//                expensesService.findAllParticipantsByExpenseId(expense.getId())
//        ));

        model.addAttribute("expenses", expenses);
        model.addAttribute("trip", trip);
        model.addAttribute("tripParticipants", tripParticipants);
        model.addAttribute("currencies", Arrays.asList(Currency.values()));

        return "expenses/expenses";
    }

    @PostMapping("/add-expense/{tripId}")
    public String addExpense(@PathVariable("tripId") Long tripId, @ModelAttribute ExpenseForm expenseForm, RedirectAttributes redirectAttributes, Model model) {
        try {
            expensesService.saveExpense(tripId, expenseForm);
            redirectAttributes.addFlashAttribute("successMessage", "Expense added successfully!");
            return "redirect:/trips/expenses/" + tripId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
            return "redirect:/trips/expenses/" + tripId;
        }
    }

    public record ExpenseForm(String title, Double amount, String currency, LocalDate date, String payerUsername, List<String> participantUsernames, List<Integer> participantAmounts) {}
}
