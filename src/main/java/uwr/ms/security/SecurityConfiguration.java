package uwr.ms.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Log4j2
public class SecurityConfiguration {

    @Bean
    @Order(0)
    SecurityFilterChain securityFilterChainForResources(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/images/**", "/css/**", "/js/**")
                .authorizeHttpRequests(c -> c.anyRequest().permitAll())
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppUserService appUserService) throws Exception {
        return http
                .formLogin(c -> c.loginPage("/login").
                        loginProcessingUrl("/authenticate")
                        .usernameParameter("user")
                        .passwordParameter("passwd")
                        .defaultSuccessUrl("/"))
                .logout(c -> c.logoutSuccessUrl("/login?logout"))
                .oauth2Login(c -> c.loginPage("/login")
                        .userInfoEndpoint(ui -> ui.userService(appUserService.oauth2LoginHandler())))
                .authorizeHttpRequests(c -> c
                        .requestMatchers("/login", "/app-user/signup", "/error").permitAll()
//                        .requestMatchers("/app-user/change-password").hasAuthority("test_role_1")
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    ApplicationListener<AuthenticationSuccessEvent> successLogger() {
        return event -> {
            log.info("success {}", event.getAuthentication());
        };
    }
}
