package uwr.ms.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uwr.ms.constant.LoginProvider;
import uwr.ms.model.entity.FriendshipEntity;
import uwr.ms.model.entity.UserEntity;
import uwr.ms.constant.FriendshipStatus;
import uwr.ms.model.repository.FriendshipRepository;
import uwr.ms.model.repository.UserEntityRepository;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Transactional
@ActiveProfiles("postgres")
public class FriendshipServiceTest {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private UserEntity requester, addressee, user1, user2, user3;

    @BeforeEach
    void setup() {
        requester = new UserEntity("requester", "password", "requester@example.com", "Requester", LoginProvider.APP, null);
        addressee = new UserEntity("addressee", "password", "addressee@example.com", "Addressee", LoginProvider.APP, null);
        userEntityRepository.save(requester);
        userEntityRepository.save(addressee);
        user1 = new UserEntity("user1", "Password1", "user1@example.com", "User One", LoginProvider.APP, "");
        user2 = new UserEntity("user2", "Password2", "user2@example.com", "User Two", LoginProvider.APP, "");
        user3 = new UserEntity("user3", "Password3", "user3@example.com", "User Three", LoginProvider.APP, "");
        userEntityRepository.saveAll(Arrays.asList(user1, user2, user3));
        createFriendship(user1, user2, FriendshipStatus.ACCEPTED);
        createFriendship(user1, user3, FriendshipStatus.ACCEPTED);
    }

    void createFriendship(UserEntity requester, UserEntity addressee, FriendshipStatus status) {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(status);
        friendshipRepository.save(friendship);
    }

    @Test
    void sendFriendRequestSuccessfully() {
        friendshipService.sendFriendRequest(requester.getUsername(), addressee.getUsername());

        FriendshipEntity result = friendshipRepository.findByRequesterUsernameAndAddresseeUsernameAndStatus(
                requester.getUsername(), addressee.getUsername(), FriendshipStatus.REQUESTED).orElse(null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FriendshipStatus.REQUESTED);
        assertThat(result.getRequester().getUsername()).isEqualTo(requester.getUsername());
        assertThat(result.getAddressee().getUsername()).isEqualTo(addressee.getUsername());
    }

    @Test
    void acceptFriendRequestSuccessfully() {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.REQUESTED);
        friendshipRepository.save(friendship);

        friendshipService.acceptFriendRequest(addressee.getUsername(), friendship.getId());

        FriendshipEntity updatedFriendship = friendshipRepository.findById(friendship.getId()).orElse(null);
        assertThat(updatedFriendship).isNotNull();
        assertThat(updatedFriendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void declineFriendRequest_Successfully() {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.REQUESTED);
        friendshipRepository.save(friendship);

        friendshipService.declineFriendRequest(addressee.getUsername(), friendship.getId());

        assertThat(friendshipRepository.findById(friendship.getId())).isEmpty();
    }

    @Test
    void sendFriendRequestToSelfThrowsException() {
        assertThatThrownBy(() -> {
            friendshipService.sendFriendRequest(requester.getUsername(), requester.getUsername());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not send friend request to yourself");
    }

    @Test
    void sendFriendRequestToExistingFriendThrowsException() {
        FriendshipEntity existingFriendship = new FriendshipEntity();
        existingFriendship.setRequester(requester);
        existingFriendship.setAddressee(addressee);
        existingFriendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(existingFriendship);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester.getUsername(), addressee.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You are already friends");
    }

    @Test
    void sendFriendRequestToBlockedUserThrowsException() {
        FriendshipEntity blockFriendship = new FriendshipEntity();
        blockFriendship.setRequester(addressee);
        blockFriendship.setAddressee(requester);
        blockFriendship.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(blockFriendship);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester.getUsername(), addressee.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You blocked user addressee");
    }

    @Test
    void sendFriendRequestWhenBlockedThrowsException() {
        FriendshipEntity blockFriendship = new FriendshipEntity();
        blockFriendship.setRequester(requester);
        blockFriendship.setAddressee(addressee);
        blockFriendship.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(blockFriendship);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester.getUsername(), addressee.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User addressee blocked your friend requests");
    }

    @Test
    void sendFriendRequestWhenAlreadyRequestedThrowsException() {
        FriendshipEntity requestedFriendship = new FriendshipEntity();
        requestedFriendship.setRequester(requester);
        requestedFriendship.setAddressee(addressee);
        requestedFriendship.setStatus(FriendshipStatus.REQUESTED);
        friendshipRepository.save(requestedFriendship);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester.getUsername(), addressee.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Friend request already sent to " + addressee.getUsername());
    }

    @Test
    void receiveFriendRequestWhenAlreadyRequestedThrowsException() {
        FriendshipEntity requestedFriendship = new FriendshipEntity();
        requestedFriendship.setRequester(addressee);
        requestedFriendship.setAddressee(requester);
        requestedFriendship.setStatus(FriendshipStatus.REQUESTED);
        friendshipRepository.save(requestedFriendship);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester.getUsername(), addressee.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User " + addressee.getUsername() + " already sent friend request to you");
    }

    @Test
    void getAllFriendsReturnsAllFriends() {
        List<UserEntity> friends = friendshipService.getAllFriends(user1.getUsername());
        assertThat(friends).hasSize(2).extracting(UserEntity::getUsername).containsExactlyInAnyOrder(user2.getUsername(), user3.getUsername());
    }

    @Test
    void removeOneFriendUpdatesFriendList() {
        friendshipService.deleteFriend(user1.getUsername(), user3.getUsername());
        List<UserEntity> updatedFriends = friendshipService.getAllFriends(user1.getUsername());
        assertThat(updatedFriends).hasSize(1).extracting(UserEntity::getUsername).containsExactly(user2.getUsername());
    }

    @Test
    void removeAllFriendsNoFriendsReturned() {
        friendshipService.deleteFriend(user1.getUsername(), user2.getUsername());
        friendshipService.deleteFriend(user1.getUsername(), user3.getUsername());
        List<UserEntity> noFriends = friendshipService.getAllFriends(user1.getUsername());
        assertThat(noFriends).isEmpty();
    }
}
