package com.goldenmemories.service.impl;

import com.goldenmemories.model.User;
import com.goldenmemories.repository.UserRepository;
import com.goldenmemories.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String fullName, String email, String phone, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email_taken");
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setLoginMethod(User.LoginMethod.LOCAL);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    @Override
    public void markEmailVerified(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmailVerified(true);
            userRepository.save(user);
        });
    }

    @Override
    public User findOrCreateOAuthUser(String email, String fullName, User.LoginMethod loginMethod) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName != null ? fullName : email);
            user.setLoginMethod(loginMethod);
            user.setEmailVerified(true); // Social login implies verified email
            return userRepository.save(user);
        });
    }
}
