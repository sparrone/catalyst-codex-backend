package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.dto.RegisterRequestDTO;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.entity.VerificationToken;
import com.separrone.awakeningbackend.repository.UserRepository;
import com.separrone.awakeningbackend.service.EmailService;
import com.separrone.awakeningbackend.service.VerificationTokenService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          VerificationTokenService verificationTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationTokenService = verificationTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        Map<String, String> fieldErrors = new HashMap<>();

        if (userRepository.existsByUsername(request.getUsername())) {
            fieldErrors.put("username", "Username already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            fieldErrors.put("email", "Email already in use");
        }

        if (!fieldErrors.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errors", fieldErrors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEnabled(false);
        userRepository.save(user);

        VerificationToken verificationToken = verificationTokenService.createTokenForUser(user);

        try {
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    user.getUsername(),
                    verificationToken.getToken()
            );
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send verification email: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String token) {
        VerificationToken verificationToken = verificationTokenService.getToken(token);

        if (verificationToken == null) {
            return ResponseEntity.badRequest().body("Invalid verification token");
        }

        if (verificationToken.getExpiration().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenService.deleteToken(verificationToken);

        URI redirectUri = URI.create("http://localhost:5173/awakening/email-verified");
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectUri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}