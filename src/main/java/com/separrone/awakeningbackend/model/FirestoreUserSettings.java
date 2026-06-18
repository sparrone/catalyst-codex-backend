package com.separrone.awakeningbackend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FirestoreUserSettings {
    
    private String userId; // Firebase UID - used as document ID
    private String theme;
    private boolean emailNotifications;
    private String timezone;
    private String language;
    
    // Constructor for creating new user settings
    public FirestoreUserSettings(String userId) {
        this.userId = userId;
        this.theme = "dark"; // Default theme
        this.emailNotifications = true;
        this.timezone = "UTC";
        this.language = "en";
    }
}