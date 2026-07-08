package com.goldenmemories.config;

import com.goldenmemories.model.User;
import com.goldenmemories.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DevDataInitializer {

    @Bean
    CommandLineRunner seedTestAccount(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "test@example.com";
            if (userRepository.existsByEmail(email)) {
                return;
            }

            User user = new User();
            user.setEmail(email);
            user.setFullName("Test User");
            user.setPhone("0123456789");
            user.setLoginMethod(User.LoginMethod.LOCAL);
            user.setRole(User.Role.USER);
            user.setPasswordHash(passwordEncoder.encode("Test1234!"));
            user.setEmailVerified(true);
            userRepository.save(user);
        };
    }
}
