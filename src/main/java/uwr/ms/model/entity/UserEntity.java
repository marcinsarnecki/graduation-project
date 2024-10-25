package uwr.ms.model.entity;

import uwr.ms.constant.LoginProvider;
import jakarta.persistence.*;
import lombok.Data;

import java.util.*;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    @Column(length = 39)
    String username;
    String password;
    String email;
    @Column(length = 39)
    String name;

    @Enumerated(value = EnumType.STRING)
    LoginProvider provider;

    @Column(name = "image_url")
    String imageUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    List<UserAuthorityEntity> userAuthorities = new ArrayList<>();

    @OneToMany(mappedBy = "requester", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<FriendshipEntity> sentFriendRequests;

    @OneToMany(mappedBy = "addressee", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<FriendshipEntity> receivedFriendRequests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TripParticipantEntity> participatingTrips = new HashSet<>();

    public UserEntity() {
    }

    public UserEntity(String username, String password, String email, String name, LoginProvider provider, String imageUrl) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.imageUrl = imageUrl;
    }

    public void addAuthority(AuthorityEntity authority) {
        if (userAuthorities.stream().anyMatch(uae -> uae.getUser().equals(this) && uae.getAuthority().equals(authority)))
            return;
        UserAuthorityEntity userAuthorityEntity = new UserAuthorityEntity(this, authority);
        this.userAuthorities.add(userAuthorityEntity);
        authority.getAssignedTo().add(userAuthorityEntity);
    }

    public void removeAuthority(AuthorityEntity authority) {
        Iterator<UserAuthorityEntity> iterator = userAuthorities.iterator();
        while (iterator.hasNext()) {
            UserAuthorityEntity next = iterator.next();
            if (next.getAuthority().equals(authority)) {
                iterator.remove();
                authority.getAssignedTo().remove(next);
                next.setUser(null);
                next.setAuthority(null);
            }
        }
    }

    public void mergeAuthorities(List<AuthorityEntity> authorities) {
        authorities.forEach(this::addAuthority);
    }

}
