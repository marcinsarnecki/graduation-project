package uwr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.model.AppUser;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.service.TripService;
import uwr.ms.service.AppUserService; // Assuming you have a UserService to fetch UserEntity by username

@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final AppUserService appUserService;

    @Autowired
    public TripController(TripService tripService, AppUserService AppUserService) {
        this.tripService = tripService;
        this.appUserService = AppUserService;
    }

    @GetMapping("/create-trip")
    public String showCreateTripForm(Model model) {
        model.addAttribute("trip", new TripEntity());
        return "trips/create_trip";
    }

    @PostMapping("/create-trip")
    public String createTrip(TripEntity trip, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            tripService.createTrip(trip, username);
            redirectAttributes.addFlashAttribute("successMessage", "Trip successfully created!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
        }
        return "redirect:/trips/create-trip";
    }
}

