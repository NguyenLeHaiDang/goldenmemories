package com.goldenmemories.security;

import com.goldenmemories.model.User;
import com.goldenmemories.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the authenticated principal to the application's {@link User} entity.
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Extracts the email from a Spring Security principal (form login or OAuth2)
     * and loads the corresponding User entity.
     */
    public Optional<User> currentUser(Object principal) {
        if (principal instanceof UserDetails ud) {
            return userRepository.findByEmail(ud.getUsername());
        }
        if (principal instanceof OAuth2User ou) {
            String email = ou.getAttribute("email");
            if (email == null) {
                // Fallback for OAuth2 providers that don't expose email
                String sub = ou.getName();
                // Try to match by sub-derived email used during OAuth2 onboarding
                return userRepository.findByEmail(sub + "@facebook.oauth")
                    .or(() -> userRepository.findByEmail(sub + "@google.oauth"));
            }
            return userRepository.findByEmail(email);
        }
        return Optional.empty();
    }
}
