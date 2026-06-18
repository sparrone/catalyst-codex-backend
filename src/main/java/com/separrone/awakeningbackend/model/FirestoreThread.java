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
public class FirestoreThread {
    
    private String id;
    private String title;
    private String categoryId;
    private String authorId; // Firebase UID
    private LocalDateTime createdAt;
    
    // Constructor for creating new threads
    public FirestoreThread(String title, String categoryId, String authorId) {
        this.title = title;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.createdAt = LocalDateTime.now();
    }
}