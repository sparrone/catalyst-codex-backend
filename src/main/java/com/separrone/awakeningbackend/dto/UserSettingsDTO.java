package com.separrone.awakeningbackend.dto;

import com.separrone.awakeningbackend.entity.UserSettings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDTO {
    private Integer threadsPerPage;
    private Integer postsPerPage;
    private Integer profilePostsPerPage;

    public static UserSettingsDTO fromEntity(UserSettings settings) {
        if (settings == null) {
            // Return default settings if none exist
            return new UserSettingsDTO(10, 10, 10);
        }
        
        return new UserSettingsDTO(
            settings.getThreadsPerPage(),
            settings.getPostsPerPage(),
            settings.getProfilePostsPerPage()
        );
    }

    // Get default settings for anonymous users
    public static UserSettingsDTO getDefaults() {
        return new UserSettingsDTO(10, 10, 10);
    }

    // Validation method
    public UserSettingsDTO validate() {
        this.threadsPerPage = Math.max(1, Math.min(50, this.threadsPerPage != null ? this.threadsPerPage : 10));
        this.postsPerPage = Math.max(1, Math.min(25, this.postsPerPage != null ? this.postsPerPage : 10));
        this.profilePostsPerPage = Math.max(1, Math.min(25, this.profilePostsPerPage != null ? this.profilePostsPerPage : 10));
        return this;
    }
}