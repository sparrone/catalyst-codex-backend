package com.separrone.awakeningbackend.service;

import com.separrone.awakeningbackend.dto.UserSettingsDTO;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.entity.UserSettings;
import com.separrone.awakeningbackend.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public UserSettingsDTO getUserSettings(User user) {
        UserSettings settings = userSettingsRepository.findByUser(user)
            .orElseGet(() -> createDefaultSettings(user));
        
        return UserSettingsDTO.fromEntity(settings);
    }

    public UserSettingsDTO updateUserSettings(User user, UserSettingsDTO settingsDTO) {
        // Validate the input
        settingsDTO.validate();
        
        UserSettings settings = userSettingsRepository.findByUser(user)
            .orElseGet(() -> createDefaultSettings(user));
        
        // Update the settings
        settings.setThreadsPerPage(settingsDTO.getThreadsPerPage());
        settings.setPostsPerPage(settingsDTO.getPostsPerPage());
        settings.setProfilePostsPerPage(settingsDTO.getProfilePostsPerPage());
        
        // Save and return
        settings = userSettingsRepository.save(settings);
        return UserSettingsDTO.fromEntity(settings);
    }

    private UserSettings createDefaultSettings(User user) {
        UserSettings settings = new UserSettings(user);
        return userSettingsRepository.save(settings);
    }

    public static UserSettingsDTO getDefaultSettings() {
        return UserSettingsDTO.getDefaults();
    }
}