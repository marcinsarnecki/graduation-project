package uwr.ms.service;

import org.springframework.beans.factory.annotation.Autowired;
import uwr.ms.constant.LoginProvider;
import uwr.ms.constant.Message;
import uwr.ms.controller.AppUserController;
import org.springframework.context.annotation.Bean;
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
import uwr.ms.exception.ValidationException;
import uwr.ms.model.entity.*;
import uwr.ms.model.repository.*;
import uwr.ms.model.AppUser;
import uwr.ms.util.ValidationUtils;

import java.util.*;

@Service
public class AppUserService implements UserDetailsManager {
    private final PasswordEncoder passwordEncoder;
    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();
    private final TripService tripService;
    private final UserEntityRepository userEntityRepository;
    private final AuthorityEntityRepository authorityEntityRepository;
    private final TripParticipantEntityRepository tripParticipantEntityRepository;
    private final TripInvitationRepository tripInvitationRepository;
    private final TripEntityRepository tripEntityRepository;
    private final FriendshipRepository friendshipRepository;

    @Autowired
    public AppUserService(PasswordEncoder passwordEncoder, TripService tripService, UserEntityRepository userEntityRepository, AuthorityEntityRepository authorityEntityRepository, TripParticipantEntityRepository tripParticipantEntityRepository, TripInvitationRepository tripInvitationRepository, TripEntityRepository tripEntityRepository, FriendshipRepository friendshipRepository) {
        this.passwordEncoder = passwordEncoder;
        this.tripService = tripService;
        this.userEntityRepository = userEntityRepository;
        this.authorityEntityRepository = authorityEntityRepository;
        this.tripParticipantEntityRepository = tripParticipantEntityRepository;
        this.tripInvitationRepository = tripInvitationRepository;
        this.tripEntityRepository = tripEntityRepository;
        this.friendshipRepository = friendshipRepository;
    }

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
                .orElseThrow(() -> new UsernameNotFoundException(String.format(Message.USER_NOT_FOUND.toString(), username)));
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2LoginHandler() {
        return userRequest -> {
            LoginProvider provider = LoginProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());
            OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(userRequest);
            AppUser appUser = AppUser.builder()
                    .username(oAuth2User.getAttribute("login"))
                    .name(oAuth2User.getAttribute("name"))
                    .email(oAuth2User.getAttribute("email"))
                    .password(UUID.randomUUID().toString())
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
        if (LoginProvider.GITHUB.equals(user.getProvider()) && userExists(user.getUsername())) // no throw with github user as this method is called every time github user logs in
            return;
        if (LoginProvider.APP.equals(user.getProvider())) {
            if (userExists(user.getUsername())) // throw because method shouldn't be called with normal user when they already exist
                throw new RuntimeException(String.format(Message.USER_ALREADY_EXISTS.toString(), user.getUsername()));
            List<String> validationErrors = new ArrayList<>();
            validationErrors.addAll(ValidationUtils.validatePassword(user.getPassword()));
            validationErrors.addAll(ValidationUtils.validateEmail(user.getEmail()));
            if (!validationErrors.isEmpty())
                throw new ValidationException(validationErrors);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        UserEntity entity = saveUserIfNotExists(user);
        if (user.getAuthorities() != null) {
            List<AuthorityEntity> authorityEntityList = user.getAuthorities().stream().map(auth -> saveAuthorityIfNotExists(auth.getAuthority(), user.getProvider())).toList();
            entity.mergeAuthorities(authorityEntityList);
        }
        userEntityRepository.save(entity);
    }

    private AuthorityEntity saveAuthorityIfNotExists(String authority, LoginProvider provider) {
        return authorityEntityRepository.findByName(authority)
                .orElseGet(() -> authorityEntityRepository.save(new AuthorityEntity(authority, provider)));
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
                        )));
    }

    @Override
    public void createUser(UserDetails user) {
        if (!(user instanceof AppUser appUser)) {
            throw new IllegalArgumentException(Message.INVALID_USER_INSTANCE.toString());
        }
        createUser(appUser);
    }

    @Override
    public void updateUser(UserDetails user) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), username)));
        List<FriendshipEntity> friendships = friendshipRepository.findByAddresseeUsernameOrRequesterUsername(username);
        friendshipRepository.deleteAll(friendships);
        List<TripInvitationEntity> invitations = tripInvitationRepository.findByReceiverUsername(username);
        tripInvitationRepository.deleteAll(invitations);
        List<TripEntity> trips = tripParticipantEntityRepository.findDistinctTripsByUserUsername(username);
        for(TripEntity trip : trips) {
            if(tripService.isUserOwner(trip, username))
                tripService.deleteTrip(trip.getId(), username);
            else
                tripService.removeParticipant(trip.getId(), username, username);
        }
        user.getUserAuthorities().clear();
        userEntityRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(String username) {
        return userEntityRepository.existsById(username);
    }

    @Transactional
    public void updateUserProfile(String username, AppUserController.EditProfileRequest editProfileRequest) {
        UserEntity user = userEntityRepository.findById(username)
                .orElseThrow(() -> new RuntimeException(String.format(Message.USER_NOT_FOUND.toString(), username)));

        List<String> validationErrors = new ArrayList<>(ValidationUtils.validateEmail(editProfileRequest.email()));
        if (!validationErrors.isEmpty())
            throw new ValidationException(validationErrors);

        if(editProfileRequest.name().isEmpty())
            throw new IllegalArgumentException(Message.NAME_CANNOT_BE_EMPTY.toString());

        if(editProfileRequest.name().length() > 39) {
            throw new IllegalArgumentException(Message.NAME_TOO_LONG.toString());
        }

        if (!editProfileRequest.imageUrl().isEmpty() && editProfileRequest.imageUrl().length() > 255)
            throw new IllegalArgumentException(Message.IMAGE_URL_TOO_LONG.toString());

        user.setImageUrl(editProfileRequest.imageUrl());
        user.setName(editProfileRequest.name());
        user.setEmail(editProfileRequest.email());
        userEntityRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        this.changePassword(username, oldPassword, newPassword);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        UserEntity user = userEntityRepository.findById(username)
                .orElseThrow(() -> new RuntimeException(String.format(Message.USER_NOT_FOUND.toString(), username)));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException(Message.INVALID_OLD_PASSWORD.toString());
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userEntityRepository.save(user);
    }

    public void validateAndChangePassword(AppUserController.ChangePasswordRequest changePasswordRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userEntityRepository.findById(username)
                .orElseThrow(() -> new RuntimeException(String.format(Message.USER_NOT_FOUND.toString(), username)));

        List<String> validationErrors = new ArrayList<>();

        if (!changePasswordRequest.newPassword().equals(changePasswordRequest.confirmNewPassword()))
            validationErrors.add(Message.PASSWORDS_DONT_MATCH.toString());

        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(), user.getPassword()))
            validationErrors.add(Message.CURRENT_PASSWORD_INCORRECT.toString());

        validationErrors.addAll(ValidationUtils.validatePassword(changePasswordRequest.newPassword()));

        if (!validationErrors.isEmpty())
            throw new ValidationException(validationErrors);

        changePassword(username, changePasswordRequest.currentPassword(), changePasswordRequest.newPassword());
    }

    @Transactional(readOnly = true)
    public void verifyPassword(String username, String password) {
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(String.format(Message.USER_NOT_FOUND.toString(), username)));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException(Message.INVALID_PASSWORD.toString());
        }
    }
}

