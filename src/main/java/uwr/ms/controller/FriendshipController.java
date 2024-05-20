package uwr.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.constant.Message;
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
    public String addFriend(@RequestParam("username") String friendUsername, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            friendshipService.sendFriendRequest(username, friendUsername);
            redirectAttributes.addFlashAttribute("successMessage", String.format(Message.FRIEND_REQUEST_SENT_SUCCESS.toString(), friendUsername));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.FRIEND_REQUEST_FAILED.toString(), e.getMessage()));
        }
        return "redirect:/friends/add-friend";
    }

    @GetMapping("/requests")
    public String getRequests(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<FriendshipEntity> friendRequests = friendshipService.listReceivedFriendRequests(username);
        List<FriendshipEntity> blockedRequests = friendshipService.listBlockedFriendRequests(username);
        model.addAttribute("friendRequests", friendRequests);
        model.addAttribute("blockedRequests", blockedRequests);
        return "friends/requests";
    }

    @PostMapping("/accept-request")
    public String acceptFriendRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            friendshipService.acceptFriendRequest(username, requestId);
            redirectAttributes.addFlashAttribute("successMessage", Message.FRIEND_REQUEST_ACCEPTED.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.FRIEND_REQUEST_ACCEPT_FAILED.toString(), e.getMessage()));
        }
        return "redirect:/friends/requests";
    }

    @PostMapping("/decline-request")
    public String declineFriendRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            friendshipService.declineFriendRequest(username, requestId);
            redirectAttributes.addFlashAttribute("successMessage", Message.FRIEND_REQUEST_DECLINED.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", Message.FRIEND_REQUEST_DECLINE_FAILED + e.getMessage());
        }
        return "redirect:/friends/requests";
    }

    @PostMapping("/block-request")
    public String blockFriendRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            friendshipService.blockFriendRequest(username, requestId);
            redirectAttributes.addFlashAttribute("successMessage", Message.FRIEND_REQUEST_BLOCKED.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.FRIEND_REQUEST_BLOCK_FAILED.toString(), e.getMessage()));
        }
        return "redirect:/friends/requests";
    }

    @PostMapping("/unblock-request")
    public String unblockRequest(@RequestParam("requestId") Long requestId, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            friendshipService.unblockFriendRequest(username, requestId);
            redirectAttributes.addFlashAttribute("successMessage", Message.USER_UNBLOCKED_SUCCESS.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.USER_UNBLOCK_FAILED.toString(), e.getMessage()));
        }
        return "redirect:/friends/requests";
    }

    @GetMapping("/my-friends")
    public String getMyFriends(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<UserEntity> friends = friendshipService.getAllFriends(username);
        model.addAttribute("friends", friends);
        return "friends/my_friends";
    }

    @PostMapping("/delete-friend")
    public String deleteFriend(@RequestParam("friendUsername") String friendUsername, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            friendshipService.deleteFriend(username, friendUsername);
            redirectAttributes.addFlashAttribute("successMessage", String.format(Message.UNFRIEND_SUCCESS.toString(), friendUsername));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.UNFRIEND_FAILED.toString(), friendUsername));
        }
        return "redirect:/friends/my-friends";
    }
}

