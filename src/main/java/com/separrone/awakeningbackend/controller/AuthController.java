package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.model.FirestoreUser;
import com.separrone.awakeningbackend.repository.FirestoreUserRepository;
import com.separrone.awakeningbackend.security.FirebaseAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private FirestoreUserRepository userRepository;

    @PostMapping("/profile/setup")
    public ResponseEntity<?> setupProfile(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(httpRequest);
            String email = FirebaseAuthUtil.getFirebaseEmail(httpRequest);
            
            String username = request.get("username");
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }

            // Check if username is already taken
            Optional<FirestoreUser> existingUser = userRepository.findByUsername(username);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(firebaseUid)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already taken"));
            }

            // Create or update user profile
            FirestoreUser user = userRepository.findById(firebaseUid)
                    .orElse(new FirestoreUser(firebaseUid, username, email));
            
            user.setUsername(username);
            user.setEmail(email);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile setup successful");
            response.put("user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to setup profile: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(httpRequest);

            Optional<FirestoreUser> userOpt = userRepository.findById(firebaseUid);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User profile not found"));
            }

            FirestoreUser user = userOpt.get();
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("enabled", user.isEnabled());
            profile.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get profile: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus(HttpServletRequest httpRequest) {
        boolean authenticated = FirebaseAuthUtil.isAuthenticated(httpRequest);
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", authenticated);
        
        if (authenticated) {
            status.put("uid", FirebaseAuthUtil.getFirebaseUid(httpRequest));
            status.put("email", FirebaseAuthUtil.getFirebaseEmail(httpRequest));
        }
        
        return ResponseEntity.ok(status);
    }
}