package uwr.ms.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import uwr.ms.constant.LoginProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Data
@Builder
public class AppUser implements UserDetails, OAuth2User {
    String username;
    String password;
    String email;
    String userId;
    String name;
    LoginProvider provider;
    String imageUrl;
    Map<String, Object> attributes;
    Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getName() {
        return Objects.nonNull(name) ? name : username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
