package com.goldenmemories.service;

import com.goldenmemories.model.User;

public interface UserService {

    /**
     * Register a new local-auth user (not yet verified).
     * Throws {@link IllegalArgumentException} if the email is already taken.
     */
    User register(String fullName, String email, String phone, String rawPassword);

    /**
     * Mark a user's email as verified and enable the account.
     */
    void markEmailVerified(String email);

    /**
     * Find or create a user from an OAuth2 social login.
     */
    User findOrCreateOAuthUser(String email, String fullName, User.LoginMethod loginMethod);
}
