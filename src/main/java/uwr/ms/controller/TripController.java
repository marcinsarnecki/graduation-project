package uwr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.constant.Message;
import uwr.ms.dto.TripDTO;
import uwr.ms.model.entity.*;
import uwr.ms.service.FriendshipService;
import uwr.ms.service.TripService;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/trips")
public class TripController {
    @Value("${GOOGLE_MAPS_API_KEY}")
    private String googleMapsApiKey;

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
            throw new AccessDeniedException("You do not have permission to edit this trip.");//todo another page for error
        model.addAttribute("trip", trip);

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedStartDate = trip.getStartDate().format(outputFormatter);
        model.addAttribute("formattedStartDate", formattedStartDate);

        return "trips/edit_trip";
    }

    @PostMapping("/update/{id}")
    public String updateTrip(@PathVariable("id") Long tripId, @ModelAttribute("trip") TripEntity trip, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!tripService.isUserOwner(tripId, username))
            throw new AccessDeniedException(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
        tripService.updateTrip(tripId, trip);
        redirectAttributes.addFlashAttribute("successMessage", Message.TRIP_UPDATED_SUCCESS);
        return "redirect:/trips/my-trips";
    }

    @GetMapping("/manage-participants/{tripId}")
    public String getManageParticipants(@PathVariable Long tripId,
                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                        Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!tripService.isUserOwner(tripId, username))
            throw new AccessDeniedException(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
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
            redirectAttributes.addFlashAttribute("successMessage", Message.USER_INVITED_SUCCESS);
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
            redirectAttributes.addFlashAttribute("successMessage", Message.PARTICIPANT_REMOVED_SUCCESS);
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
            redirectAttributes.addFlashAttribute("successMessage", Message.INVITATION_ACCEPTED_SUCCESS);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.ERROR_ACCEPTING_INVITATION.toString(), e.getMessage()));
        }
        return "redirect:/trips/trip-invitations";
    }

    @PostMapping("/decline-invitation")
    public String declineInvitation(@RequestParam("invitationId") Long invitationId, RedirectAttributes redirectAttributes) {
        try {
            tripService.declineInvitation(invitationId);
            redirectAttributes.addFlashAttribute("successMessage", Message.INVITATION_DECLINED_SUCCESS);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.ERROR_DECLINING_INVITATION.toString(), e.getMessage()));
        }
        return "redirect:/trips/trip-invitations";
    }

    @GetMapping("/view/{id}")
    public String getViewTrip(@PathVariable("id") Long tripId, Model model) {
        TripEntity trip = tripService.findTripById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVALID_TRIP_ID + String.valueOf(tripId)));
        model.addAttribute("trip", trip);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        model.addAttribute("formattedStartDate", trip.getStartDate().format(dateFormatter));

        Set<String> potentialFriendsUsernames = friendshipService
                .getAllPotentialFriendsAmongTripParticipants(trip.getId(), username)
                .stream()
                .map(UserEntity::getUsername)
                .collect(Collectors.toSet());
        List<ParticipantDTO> participantDTOs = tripService.findAllParticipantsByTripId(trip.getId()).stream()
                .map(p -> new ParticipantDTO(p.getUser().getUsername(), p.getUser().getName(), potentialFriendsUsernames.contains(p.getUser().getUsername()))).toList();

        model.addAttribute("participants", participantDTOs);
        model.addAttribute("events", getEventsSortedByDateAndTime(trip.getEvents()));
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "trips/view_trip";
    }

    @GetMapping("/manage-events/{tripId}")
    public String manageEvents(@PathVariable Long tripId, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!tripService.isUserOwner(tripId, username))
            throw new AccessDeniedException(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
        TripEntity trip = tripService.findTripById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVALID_TRIP_ID + String.valueOf(tripId)));
        model.addAttribute("trip", trip);
        model.addAttribute("events", getEventsSortedByDateAndTime(trip.getEvents()));
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "trips/manage_events";
    }

    @PostMapping("/manage-events/{tripId}/save")
    public String saveEvent(@ModelAttribute("event") EventEntity event, @PathVariable Long tripId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripEntity trip = tripService.findTripById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(Message.INVALID_TRIP_ID + String.valueOf(tripId)));
        if (!tripService.isUserOwner(tripId, username))
            throw new AccessDeniedException(Message.EDIT_TRIP_PERMISSION_DENIED.toString());
        if(event.getDate() == null || event.getTime() == null)
            throw new IllegalArgumentException("Enter proper date and time");

        try {
            tripService.addEventToTrip(event, trip);
            redirectAttributes.addFlashAttribute("successMessage", Message.EVENT_SAVED_SUCCESS);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
        }
        return "redirect:/trips/manage-events/" + tripId;
    }

    private List<EventEntity> getEventsSortedByDateAndTime(List<EventEntity> events) {
        return events.stream()
                .sorted(Comparator.comparing(EventEntity::getDate)
                        .thenComparing(EventEntity::getTime))
                .collect(Collectors.toList());
    }

    @PostMapping("/{tripId}/delete-event/{eventId}")
    public String deleteEvent(@PathVariable Long tripId, @PathVariable Long eventId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            tripService.deleteEvent(tripId, eventId, username);
            redirectAttributes.addFlashAttribute("successMessage", Message.EVENT_DELETED_SUCCESS);
            return "redirect:/trips/manage-events/" + tripId;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
            return "redirect:/error";
        }
    }

    public record ParticipantDTO(String username, String name, boolean isPotentialFriend) {}
}

