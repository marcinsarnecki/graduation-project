package uwr.ms.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uwr.ms.constant.LoginProvider;
import uwr.ms.model.AppUser;
import uwr.ms.service.AppUserService;

import java.util.List;

@Service
public class UserCreatorService {
    PasswordEncoder passwordEncoder;
    AppUserService appUserService;

    @Autowired
    public UserCreatorService(PasswordEncoder passwordEncoder, AppUserService appUserService) {
        this.passwordEncoder = passwordEncoder;
        this.appUserService = appUserService;
    }

    @PostConstruct
    @Transactional
    public void createHardcodedUsers() {
        var user1 = AppUser.builder().username("user1").email("user1@example.com").password(passwordEncoder.encode("abcd1234!")).provider(LoginProvider.APP).authorities(List.of(new SimpleGrantedAuthority("test_role_2"), new SimpleGrantedAuthority("STANDARD_USER"))).build();
        var user2 = AppUser.builder().username("user2").email("user2@example.com").password(passwordEncoder.encode("abcd1234!!")).provider(LoginProvider.APP).authorities(List.of(new SimpleGrantedAuthority("test_role_1"), new SimpleGrantedAuthority("STANDARD_USER"))).build();
        if(!appUserService.userExists(user1.getUsername()))
            appUserService.createUser(user1);
        if(!appUserService.userExists(user2.getUsername()))
            appUserService.createUser(user2);

    }
}
