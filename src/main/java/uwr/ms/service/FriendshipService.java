package uwr.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.FriendshipStatus;
import uwr.ms.constant.Message;
import uwr.ms.model.entity.FriendshipEntity;
import uwr.ms.model.entity.TripParticipantEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.FriendshipRepository;
import uwr.ms.model.repository.UserEntityRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserEntityRepository userEntityRepository;
    private final TripService tripService;

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository, UserEntityRepository userEntityRepository, TripService tripService) {
        this.friendshipRepository = friendshipRepository;
        this.userEntityRepository = userEntityRepository;
        this.tripService = tripService;
    }

    @Transactional
    public void sendFriendRequest(String requesterUsername, String addresseeUsername) {
        UserEntity requester = userEntityRepository.findById(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException(Message.REQUESTER_NOT_FOUND.toString()));
        UserEntity addressee = userEntityRepository.findById(addresseeUsername)
                .orElseThrow(() -> new IllegalArgumentException(Message.ADDRESSEE_NOT_FOUND.toString()));
        if(requesterUsername.equals(addresseeUsername))
            throw new IllegalArgumentException(Message.CANNOT_FRIEND_YOURSELF.toString());
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, addresseeUsername, FriendshipStatus.ACCEPTED).isPresent())
            throw new IllegalStateException(Message.ALREADY_FRIENDS.toString());
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(addresseeUsername, requesterUsername, FriendshipStatus.BLOCKED).isPresent())
            throw new IllegalStateException(String.format(Message.BLOCKED_BY_YOU.toString(), addresseeUsername));
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, addresseeUsername, FriendshipStatus.BLOCKED).isPresent())
            throw new IllegalStateException(String.format(Message.BLOCKED_BY_USER.toString(), addresseeUsername));
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, addresseeUsername, FriendshipStatus.REQUESTED).isPresent())
            throw new IllegalStateException(String.format(Message.REQUEST_ALREADY_SENT.toString(), addresseeUsername));
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(addresseeUsername, requesterUsername, FriendshipStatus.REQUESTED).isPresent())
            throw new IllegalStateException(String.format(Message.REQUEST_ALREADY_RECEIVED.toString(), addresseeUsername));

        FriendshipEntity friendRequest = new FriendshipEntity();
        friendRequest.setRequester(requester);
        friendRequest.setAddressee(addressee);
        friendRequest.setStatus(FriendshipStatus.REQUESTED);
        friendshipRepository.save(friendRequest);
    }

    @Transactional(readOnly = true)
    public List<FriendshipEntity> listReceivedFriendRequests(String username) {
        return friendshipRepository.findByAddresseeUsernameAndStatus(username, FriendshipStatus.REQUESTED);
    }

    @Transactional
    public void acceptFriendRequest(String username, Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(Message.FRIEND_REQUEST_NOT_FOUND.toString()));
        if(!username.equals(request.getAddressee().getUsername()))
            throw new AccessDeniedException(Message.ACCESS_DENIED_ACCEPT_FRIEND_REQUEST.toString());
        request.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(request);
    }

    @Transactional
    public void declineFriendRequest(String username, Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(Message.FRIEND_REQUEST_NOT_FOUND.toString()));
        if(!username.equals(request.getAddressee().getUsername()))
            throw new AccessDeniedException(Message.ACCESS_DENIED_DECLINE_FRIEND_REQUEST.toString());
        friendshipRepository.delete(request);
    }

    @Transactional
    public void blockFriendRequest(String username, Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(Message.FRIEND_REQUEST_NOT_FOUND.toString()));
        if(!username.equals(request.getAddressee().getUsername()))
            throw new AccessDeniedException(Message.ACCESS_DENIED_BLOCK_FRIEND_REQUEST.toString());
        request.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(request);
    }

    @Transactional
    public void unblockFriendRequest(String username, Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(Message.FRIEND_REQUEST_NOT_FOUND.toString()));
        if(!username.equals(request.getAddressee().getUsername()))
            throw new AccessDeniedException(Message.ACCESS_DENIED_UNBLOCK_FRIEND_REQUEST.toString());
        friendshipRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllFriends(String username) {
        return userEntityRepository.findFriendsByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllPotentialFriendsAmongTripParticipants(Long tripId, String username) {
        return userEntityRepository.findPotentialFriendsAmongParticipants(tripId, username);
    }

    public Page<UserEntity> getFriendsPageable(String username, Pageable pageable) {
        return userEntityRepository.findFriendsByUsername(username, pageable);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAvailableFriends(String username, Long tripId) {
        List<UserEntity> allFriends = getAllFriends(username);
        Set<UserEntity> participants = tripService.findAllParticipantsByTripId(tripId).stream().map(TripParticipantEntity::getUser).collect(Collectors.toSet());

        return allFriends.stream()
                .filter(friend -> !participants.contains(friend))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendshipEntity> listBlockedFriendRequests(String username) {
        return friendshipRepository.findByAddresseeUsernameAndStatus(username, FriendshipStatus.BLOCKED);
    }

    @Transactional
    public void deleteFriend(String requesterUsername, String friendUsername) {
        Optional<FriendshipEntity> friendship = friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, friendUsername, FriendshipStatus.ACCEPTED);
        if (friendship.isEmpty())
            friendship = friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(friendUsername, requesterUsername, FriendshipStatus.ACCEPTED);
        if (friendship.isEmpty())
            throw new RuntimeException();
        friendship.ifPresent(friendshipRepository::delete);
    }
}