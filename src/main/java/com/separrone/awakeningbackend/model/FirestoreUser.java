package com.separrone.awakeningbackend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FirestoreUser {
    
    private String id; // Firebase UID
    private String username;
    private String email;
    private boolean enabled;
    private LocalDateTime createdAt;
    
    // Constructor for creating new users
    public FirestoreUser(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.enabled = true; // Firebase Auth handles email verification
        this.createdAt = LocalDateTime.now();
    }
}