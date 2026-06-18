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
public class FirestorePost {
    
    private String id;
    private String content;
    private String threadId;
    private String authorId; // Firebase UID
    private LocalDateTime createdAt;
    
    // Constructor for creating new posts
    public FirestorePost(String content, String threadId, String authorId) {
        this.content = content;
        this.threadId = threadId;
        this.authorId = authorId;
        this.createdAt = LocalDateTime.now();
    }
}