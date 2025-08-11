package com.separrone.awakeningbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "threads_per_page")
    private Integer threadsPerPage = 10;

    @Column(name = "posts_per_page")
    private Integer postsPerPage = 10;

    @Column(name = "profile_posts_per_page")
    private Integer profilePostsPerPage = 10;

    // Constructor for creating default settings
    public UserSettings(User user) {
        this.user = user;
        this.threadsPerPage = 10;
        this.postsPerPage = 10;
        this.profilePostsPerPage = 10;
    }

    // Validation methods
    public void setThreadsPerPage(Integer threadsPerPage) {
        this.threadsPerPage = Math.max(1, Math.min(50, threadsPerPage));
    }

    public void setPostsPerPage(Integer postsPerPage) {
        this.postsPerPage = Math.max(1, Math.min(25, postsPerPage));
    }

    public void setProfilePostsPerPage(Integer profilePostsPerPage) {
        this.profilePostsPerPage = Math.max(1, Math.min(25, profilePostsPerPage));
    }
}