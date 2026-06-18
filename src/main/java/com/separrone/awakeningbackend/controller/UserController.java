package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.model.FirestoreUser;
import com.separrone.awakeningbackend.model.FirestoreUserSettings;
import com.separrone.awakeningbackend.model.FirestorePost;
import com.separrone.awakeningbackend.repository.FirestoreUserRepository;
import com.separrone.awakeningbackend.repository.FirestoreUserSettingsRepository;
import com.separrone.awakeningbackend.repository.FirestorePostRepository;
import com.separrone.awakeningbackend.security.FirebaseAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/me")
public class UserController {

    @Autowired
    private FirestoreUserRepository userRepository;

    @Autowired
    private FirestoreUserSettingsRepository userSettingsRepository;

    @Autowired
    private FirestorePostRepository postRepository;

    @GetMapping
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            FirebaseAuthUtil.requireAuthentication(request);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(request);

            Optional<FirestoreUser> userOpt = userRepository.findById(firebaseUid);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User profile not found"));
            }

            FirestoreUser user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "enabled", user.isEnabled(),
                    "createdAt", user.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get current user: " + e.getMessage()));
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<?> getUserPosts(HttpServletRequest request) {
        try {
            FirebaseAuthUtil.requireAuthentication(request);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(request);

            List<FirestorePost> posts = postRepository.findByAuthorId(firebaseUid);
            
            return ResponseEntity.ok(Map.of(
                    "posts", posts,
                    "count", posts.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user posts: " + e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getUserSettings(HttpServletRequest request) {
        try {
            FirebaseAuthUtil.requireAuthentication(request);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(request);

            Optional<FirestoreUserSettings> settingsOpt = userSettingsRepository.findByUserId(firebaseUid);
            FirestoreUserSettings settings = settingsOpt.orElse(new FirestoreUserSettings(firebaseUid));
            
            Map<String, Object> response = new HashMap<>();
            response.put("theme", settings.getTheme());
            response.put("emailNotifications", settings.isEmailNotifications());
            response.put("timezone", settings.getTimezone());
            response.put("language", settings.getLanguage());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user settings: " + e.getMessage()));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateUserSettings(@RequestBody Map<String, Object> settingsData, HttpServletRequest request) {
        try {
            FirebaseAuthUtil.requireAuthentication(request);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(request);

            FirestoreUserSettings settings = userSettingsRepository.findByUserId(firebaseUid)
                    .orElse(new FirestoreUserSettings(firebaseUid));

            // Update settings from request
            if (settingsData.containsKey("theme")) {
                settings.setTheme((String) settingsData.get("theme"));
            }
            if (settingsData.containsKey("emailNotifications")) {
                settings.setEmailNotifications((Boolean) settingsData.get("emailNotifications"));
            }
            if (settingsData.containsKey("timezone")) {
                settings.setTimezone((String) settingsData.get("timezone"));
            }
            if (settingsData.containsKey("language")) {
                settings.setLanguage((String) settingsData.get("language"));
            }

            userSettingsRepository.save(settings);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Settings updated successfully");
            response.put("theme", settings.getTheme());
            response.put("emailNotifications", settings.isEmailNotifications());
            response.put("timezone", settings.getTimezone());
            response.put("language", settings.getLanguage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user settings: " + e.getMessage()));
        }
    }
}