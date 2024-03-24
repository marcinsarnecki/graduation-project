package uwr.ms.security;

import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uwr.ms.service.AppUserService;

@Service
@Value
public class UserCreatorService {
    PasswordEncoder passwordEncoder;
    AppUserService appUserService;

    @PostConstruct
    public void createHardcodedUsers() {
//        var user1 = AppUser.builder().username("user1").email("user1@example.com").password(passwordEncoder.encode("abcd1234!")).provider(LoginProvider.APP).authorities(List.of(new SimpleGrantedAuthority("test_role_2"), new SimpleGrantedAuthority("STANDARD_USER"))).build();
//        var user2 = AppUser.builder().username("user2").email("user2@example.com").password(passwordEncoder.encode("abcd1234!!")).provider(LoginProvider.APP).authorities(List.of(new SimpleGrantedAuthority("test_role_1"), new SimpleGrantedAuthority("STANDARD_USER"))).build();
//        appUserService.deleteUser(user1.username);
//        appUserService.deleteUser(user2.username);
//        appUserService.createUser(user1);
//        appUserService.createUser(user2);
    }
}
