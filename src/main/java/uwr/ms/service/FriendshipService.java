package uwr.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.FriendshipStatus;
import uwr.ms.model.entity.FriendshipEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.model.repository.FriendshipRepository;
import uwr.ms.model.repository.UserEntityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserEntityRepository userEntityRepository;

    @Autowired
    public FriendshipService(FriendshipRepository friendshipRepository, UserEntityRepository userEntityRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userEntityRepository = userEntityRepository;
    }

    @Transactional
    public void sendFriendRequest(String requesterUsername, String addresseeUsername) {
        UserEntity requester = userEntityRepository.findById(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        UserEntity addressee = userEntityRepository.findById(addresseeUsername)
                .orElseThrow(() -> new IllegalArgumentException("Addressee not found"));
        if(requesterUsername.equals(addresseeUsername))
            throw new IllegalArgumentException("Can not send friend request to yourself");
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, addresseeUsername, FriendshipStatus.ACCEPTED).isPresent())
            throw new IllegalStateException("You are already friends");
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(addresseeUsername, requesterUsername, FriendshipStatus.BLOCKED).isPresent())
            throw new IllegalStateException(String.format("You blocked user %s", addresseeUsername));
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, addresseeUsername, FriendshipStatus.BLOCKED).isPresent())
            throw new IllegalStateException(String.format("User %s blocked your friend requests", addresseeUsername));
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(requesterUsername, addresseeUsername, FriendshipStatus.REQUESTED).isPresent())
            throw new IllegalStateException(String.format("Friend request already sent to %s", addresseeUsername));
        if (friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(addresseeUsername, requesterUsername, FriendshipStatus.REQUESTED).isPresent())
            throw new IllegalStateException(String.format("User %s already sent friend request to you", addresseeUsername));

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
    public void acceptFriendRequest(Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        request.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(request);
    }

    @Transactional
    public void declineFriendRequest(Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
//        request.setStatus(FriendshipStatus.DECLINED);
//        friendshipRepository.save(request);
        friendshipRepository.delete(request); //delete request from db, user can try to request again
    }

    @Transactional
    public void blockFriendRequest(Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        request.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(request);
    }

    @Transactional
    public void unblockFriendRequest(Long requestId) {
        FriendshipEntity request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        friendshipRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllFriends(String username) {
        List<UserEntity> friends = new ArrayList<>();
        friendshipRepository.findByRequesterUsernameAndStatus(username, FriendshipStatus.ACCEPTED).forEach(friendshipEntity -> {
            friends.add(friendshipEntity.getAddressee());
        });
        friendshipRepository.findByAddresseeUsernameAndStatus(username, FriendshipStatus.ACCEPTED).forEach(friendshipEntity -> {
            friends.add(friendshipEntity.getRequester());
        });
        return friends;
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
        friendship.ifPresent(friendshipRepository::delete);
    }
}