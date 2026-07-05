package com.goldenmemories.security;

import com.goldenmemories.model.User;
import com.goldenmemories.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After a successful OAuth2 login, ensure the user exists in the database
 * and then redirect to the dashboard.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;

    public OAuth2LoginSuccessHandler(UserService userService) {
        super("/dashboard");
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauthUser = oauthToken.getPrincipal();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            String email = oauthUser.getAttribute("email");
            String name  = oauthUser.getAttribute("name");

            // Facebook may not return an email if the user hasn't granted it;
            // fall back to the provider sub-ID so the account can still be created.
            if (email == null) {
                String sub = oauthUser.getName(); // provider-assigned ID
                email = sub + "@" + registrationId + ".oauth";
            }

            User.LoginMethod method = "google".equalsIgnoreCase(registrationId)
                ? User.LoginMethod.GOOGLE
                : User.LoginMethod.FACEBOOK;

            userService.findOrCreateOAuthUser(email, name, method);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
