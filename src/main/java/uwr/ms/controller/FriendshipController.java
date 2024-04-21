package uwr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.model.entity.FriendshipEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.service.FriendshipService;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @Autowired
    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping("/add-friend")
    public String getAddFriend() {
        return "friends/add_friend";
    }

    @PostMapping("/add-friend")
    public String postAddFriend(@RequestParam("username") String friendUsername, Principal principal, RedirectAttributes redirectAttributes) {
        String requesterUsername = principal.getName();
        try {
            friendshipService.sendFriendRequest(requesterUsername, friendUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request sent successfully to " + friendUsername + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Failed to send friend request: " + e.getMessage());
        }
        return "redirect:/friends/add-friend";
    }

    @GetMapping("/requests")
    public String getRequests(Model model, Principal principal) {
        String username = principal.getName();
        List<FriendshipEntity> friendRequests = friendshipService.listReceivedFriendRequests(username);
        List<FriendshipEntity> blockedRequests = friendshipService.listBlockedFriendRequests(username); // Assume this method exists
        model.addAttribute("friendRequests", friendRequests);
        model.addAttribute("blockedRequests", blockedRequests);
        return "friends/requests";
    }

    @PostMapping("/accept-request")
    public String postAcceptFriendRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        try {
            friendshipService.acceptFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request accepted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Failed to accept friend request: " + e.getMessage());
        }
        return "redirect:/friends/requests";
    }

    @PostMapping("/decline-request")
    public String postDeclineFriendRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        try {
            friendshipService.declineFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request declined.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Failed to decline friend request: " + e.getMessage());
        }
        return "redirect:/friends/requests";
    }

    @PostMapping("/block-request")
    public String postBlockFriendRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        try {
            friendshipService.blockFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Friend request declined.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Failed to decline friend request: " + e.getMessage());
        }
        return "redirect:/friends/requests";
    }

    @PostMapping("/unblock-request")
    public String unblockRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        try {
            friendshipService.unblockFriendRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "User unblocked successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Failed to unblock user: " + e.getMessage());
        }
        return "redirect:/friends/requests";
    }
    
    @GetMapping("/my-friends")
    public String getMyFriends(Model model,
                               @RequestParam(name = "page", defaultValue = "0") int page,
                               Principal principal) {
        String username = principal.getName();
        Pageable pageable = PageRequest.of(page, 10);
        Page<UserEntity> friendsPage = friendshipService.getFriendsPageable(username, pageable);
        model.addAttribute("friendsPage", friendsPage);
        return "friends/my_friends";
    }

    @PostMapping("/delete-friend")
    public String deleteFriend(@RequestParam("friendUsername") String friendUsername, Principal principal, RedirectAttributes redirectAttributes) {
        String username = principal.getName();
        try {
            friendshipService.deleteFriend(username, friendUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully unfriended " + friendUsername + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", "Failed to unfriend " + friendUsername + ".");
        }
        return "redirect:/friends/my-friends";
    }
}