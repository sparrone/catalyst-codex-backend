package com.separrone.awakeningbackend.service;

import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword(passwordEncoder.encode("testpass")); // bcrypt
            user.setEmail("testuser@separrone.com");
            user.setEnabled(true);
            userRepository.save(user);
            System.out.println("✅ Seeded testuser with password 'testpass'");
        }
    }
}
