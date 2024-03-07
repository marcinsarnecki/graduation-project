package uwr.ms.security;

import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Value
public class UserCreatorService {
    PasswordEncoder passwordEncoder;
    AppUserService appUserService;

    @PostConstruct
    public void createHardcodedUsers() {
        var user1 = AppUser.builder().username("user1").password(passwordEncoder.encode("1234")).provider(LoginProvider.APP).authorities(List.of(new SimpleGrantedAuthority("test_role_2"))).build();
        var user2 = AppUser.builder().username("user2").password(passwordEncoder.encode("12345")).provider(LoginProvider.APP).authorities(List.of(new SimpleGrantedAuthority("test_role_1"))).build();
        appUserService.createUser(user1);
        appUserService.createUser(user2);
    }
}
