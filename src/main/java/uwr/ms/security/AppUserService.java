package uwr.ms.security;

import uwr.ms.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AppUserService implements UserDetailsManager {
    private final PasswordEncoder passwordEncoder;
    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();
    private final UserEntityRepository userEntityRepository;
    private final AuthorityEntityRepository authorityEntityRepository;
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userEntityRepository
                .findById(username)
                .map(ue -> AppUser
                        .builder()
                        .username(ue.getUsername())
                        .password(ue.getPassword())
                        .name(ue.getName())
                        .email(ue.getEmail())
                        .imageUrl(ue.getImageUrl())
                        .provider(ue.getProvider())
                        .authorities(ue.getUserAuthorities().stream().map(auth -> new SimpleGrantedAuthority(auth.getAuthority().getName())).toList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(String.format("%s not found", username)));
    }


    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2LoginHandler() {
        return userRequest -> {
            LoginProvider provider = LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
            OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(userRequest);
            AppUser appUser = AppUser.builder()
                    .username(oAuth2User.getAttribute("login"))
                    .name(oAuth2User.getAttribute("name"))
                    .email(oAuth2User.getAttribute("email"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .userId(oAuth2User.getName())
                    .provider(provider)
                    .imageUrl(oAuth2User.getAttribute("avatar_url"))
                    .attributes(oAuth2User.getAttributes())
                    .authorities(oAuth2User.getAuthorities())
                    .build();
            createUser(appUser);
            return appUser;
        };
    }

    @Transactional
    public void createUser(AppUser user) {
        UserEntity entity = saveUserIfNotExists(user);
        if(user.authorities != null) {
            List<AuthorityEntity> authorityEntityList = user.authorities.stream().map(auth -> saveAuthorityIfNotExists(auth.getAuthority(), user.getProvider())).toList();
            entity.updateAuthorities(authorityEntityList);
        }
        userEntityRepository.save(entity);
    }

    private AuthorityEntity saveAuthorityIfNotExists(String authority, LoginProvider provider) {
        return authorityEntityRepository.findByName(authority).orElseGet(() -> authorityEntityRepository.save(new AuthorityEntity(authority, provider)));
    }

    private UserEntity saveUserIfNotExists(AppUser user) {
        return userEntityRepository
                .findById(user.getUsername())
                .orElseGet(() -> userEntityRepository
                        .save(new UserEntity(
                                user.getUsername(),
                                user.getPassword(),
                                user.getEmail(),
                                user.getName(),
                                user.getProvider(),
                                user.getImageUrl()
                        )))
                ;
    }

    @Override
    public void createUser(UserDetails user) {
        if (!(user instanceof AppUser appUser)) {
            throw new IllegalArgumentException("User must be an instance of AppUser");
        }
        createUser(appUser);
    }


    @Override
    public void updateUser(UserDetails user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String username) {
        if(userExists(username)) {
            userEntityRepository.deleteById(username);
        }
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("New password and old password don't match"); //todo handle
        }
        userEntityRepository
                .findById(currentUser.getUsername())
                .ifPresent(ue -> ue.setPassword(passwordEncoder.encode(newPassword)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(String username) {
        return userEntityRepository.existsById(username);
    }
}
