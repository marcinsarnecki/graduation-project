package uwr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.dto.TripDTO;
import uwr.ms.model.entity.TripEntity;
import uwr.ms.model.entity.TripInvitationEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.service.FriendshipService;
import uwr.ms.service.TripService;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final FriendshipService friendshipService;

    @Autowired
    public TripController(TripService tripService, FriendshipService friendshipService) {
        this.tripService = tripService;
        this.friendshipService = friendshipService;
    }

    @GetMapping("/create-trip")
    public String getCreateTrip(Model model) {
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

    @GetMapping("/my-trips")
    public String getMyTrips(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TripDTO> trips = tripService.findAllTripsByUser(username);
        model.addAttribute("trips", trips);
        return "trips/my_trips";
    }

    @GetMapping("/edit/{id}")
    public String getEditTrip(@PathVariable("id") Long id, Model model) {
        TripEntity trip = tripService.findTripById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid trip Id:" + id));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!tripService.isUserOwner(trip.getId(), username))
            throw new AccessDeniedException("You do not have permission to edit this trip.");
        model.addAttribute("trip", trip);
        return "trips/edit_trip";
    }

    @PostMapping("/update/{id}")
    public String updateTrip(@PathVariable("id") Long id, @ModelAttribute("trip") TripEntity trip, RedirectAttributes redirectAttributes) {
        tripService.updateTrip(id, trip);
        redirectAttributes.addFlashAttribute("successMessage", "Trip updated successfully!");
        return "redirect:/trips/my-trips";
    }

    @GetMapping("/manage-participants/{tripId}")
    public String getManageParticipants(@PathVariable Long tripId,
                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                        Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!tripService.isUserOwner(tripId, username))
            throw new AccessDeniedException("You do not have permission to edit this trip.");
        Pageable pageable = PageRequest.of(page, 10);
        Page<TripParticipantEntity> participantsPage = tripService.findParticipantsByTrip(tripId, pageable);
        List<UserEntity> friends = friendshipService.getAvailableFriends(username, tripId);
        friends.sort(Comparator.comparing(UserEntity::getName, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("tripOwner", username);
        model.addAttribute("tripId", tripId);
        model.addAttribute("tripName", tripService.findTripById(tripId).get().getName());
        model.addAttribute("participantsPage", participantsPage);
        model.addAttribute("friends", friends);
        return "trips/manage_participants";
    }

    @PostMapping("/add-participant/{tripId}")
    public String addParticipant(@PathVariable Long tripId, @RequestParam String username, RedirectAttributes redirectAttributes) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            tripService.sendTripInvitation(tripId, username, currentUsername);
            redirectAttributes.addFlashAttribute("successMessage", "User invited successfully!");
        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
        }
        return "redirect:/trips/manage-participants/" + tripId;
    }

    @PostMapping("/remove-participant/{tripId}/{participantId}")
    public String removeParticipant(@PathVariable Long tripId, @PathVariable Long participantId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            tripService.removeParticipant(tripId, participantId, username);
            redirectAttributes.addFlashAttribute("successMessage", "Participant removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/trips/manage-participants/" + tripId;
    }

    @GetMapping("/trip-invitations")
    public String getTripInvitations(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TripInvitationEntity> invitations = tripService.getTripInvitations(username);
        model.addAttribute("invitations", invitations);
        return "trips/trip_invitations";
    }

    @PostMapping("/accept-invitation")
    public String acceptInvitation(@RequestParam("invitationId") Long invitationId, RedirectAttributes redirectAttributes) {
        try {
            tripService.acceptInvitation(invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation accepted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Error accepting invitation: " + e.getMessage());
        }
        return "redirect:/trips/trip-invitations";
    }

    @PostMapping("/decline-invitation")
    public String declineInvitation(@RequestParam("invitationId") Long invitationId, RedirectAttributes redirectAttributes) {
        try {
            tripService.declineInvitation(invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation declined successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Error declining invitation: " + e.getMessage());
        }
        return "redirect:/trips/trip-invitations";
    }

}

