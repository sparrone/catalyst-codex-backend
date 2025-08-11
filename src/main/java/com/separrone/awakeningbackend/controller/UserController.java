package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.dto.ChangeEmailRequestDTO;
import com.separrone.awakeningbackend.dto.ChangePasswordRequestDTO;
import com.separrone.awakeningbackend.dto.UserPostDTO;
import com.separrone.awakeningbackend.dto.UserSettingsDTO;
import com.separrone.awakeningbackend.entity.Post;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.entity.VerificationToken;
import com.separrone.awakeningbackend.exception.ResourceNotFoundException;
import com.separrone.awakeningbackend.exception.UnauthorizedException;
import com.separrone.awakeningbackend.repository.PostRepository;
import com.separrone.awakeningbackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.separrone.awakeningbackend.service.EmailService;
import com.separrone.awakeningbackend.service.UserSettingsService;
import com.separrone.awakeningbackend.service.VerificationTokenService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;

@RestController
@RequestMapping("/me")
public class UserController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;
    private final UserSettingsService userSettingsService;

    public UserController(UserRepository userRepository,
                          PostRepository postRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          VerificationTokenService verificationTokenService,
                          UserSettingsService userSettingsService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationTokenService = verificationTokenService;
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        System.out.println("🔍 Session ID: " + session.getId());

        // Get authentication from Spring Security context instead of manual session attribute
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("🔍 Authentication: " + auth);
        System.out.println("🔍 Principal: " + (auth != null ? auth.getName() : "null"));

        User user = getAuthenticatedUser();
        System.out.println("🔍 Found user: " + user.getUsername() + " | enabled: " + user.isEnabled());

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequestDTO request,
                                            HttpSession session) throws MessagingException {
        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Current password is incorrect");
        }

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("New password cannot be empty");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        emailService.sendPasswordChangeEmail(user.getEmail());

        return ResponseEntity.ok("Password updated successfully");
    }

    @PutMapping("/email")
    public ResponseEntity<?> changeEmail(@RequestBody ChangeEmailRequestDTO request,
                                         HttpSession session) throws MessagingException {
        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Current password is incorrect");
        }

        if (request.getNewEmail() == null || request.getNewEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("New email cannot be empty");
        }

        // Generate token for new email
        VerificationToken token = verificationTokenService.createTokenForEmailChange(user, request.getNewEmail());

        // Send verification email to new email
        emailService.sendEmailChangeVerification(user.getUsername(), request.getNewEmail(), token.getToken());

        // Notify old email
        emailService.sendEmailChangeNotification(user.getUsername(), user.getEmail(), request.getNewEmail());

        return ResponseEntity.ok("A verification link has been sent to your new email. Please verify to complete the change.");
    }

    @GetMapping("/settings")
    public ResponseEntity<UserSettingsDTO> getUserSettings() {
        User user = getAuthenticatedUser();
        UserSettingsDTO settings = userSettingsService.getUserSettings(user);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<UserSettingsDTO> updateUserSettings(@RequestBody UserSettingsDTO settingsDTO) {
        User user = getAuthenticatedUser();
        UserSettingsDTO updatedSettings = userSettingsService.updateUserSettings(user, settingsDTO);
        return ResponseEntity.ok(updatedSettings);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}