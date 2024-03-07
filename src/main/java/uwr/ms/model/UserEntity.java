package uwr.ms.model;

import uwr.ms.security.LoginProvider;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    String username;
    String password;
    String email;
    String name;

    @Enumerated(value = EnumType.STRING)
    LoginProvider provider;

    @Column(name = "image_url")
    String imageUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    List<UserAuthorityEntity> userAuthorities = new ArrayList<>();


    public UserEntity() {}

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

    public void updateAuthorities(List<AuthorityEntity> authorities) {
        var toRemove = this
                .userAuthorities
                .stream()
                .filter(uae -> !authorities.contains(uae.getAuthority()))
                .toList();
        toRemove.forEach(uae -> this.removeAuthority(uae.getAuthority()));
        authorities.forEach(this::addAuthority);
    }

}
