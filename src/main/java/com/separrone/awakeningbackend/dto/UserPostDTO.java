package com.separrone.awakeningbackend.dto;

import com.separrone.awakeningbackend.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPostDTO {
    private Long postId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    
    // Thread info
    private Long threadId;
    private String threadTitle;
    private Boolean threadLocked;
    
    // Category info
    private Long categoryId;
    private String categoryName;
    
    // Pagination info for deep linking
    private Integer pageNumber;    // Which page this post is on in the thread
    private Integer positionOnPage; // Position of this post on that page (for scrolling)
    
    public static UserPostDTO fromEntity(Post post, Integer pageNumber, Integer positionOnPage) {
        return new UserPostDTO(
            post.getId(),
            post.getContent(),
            post.getCreatedAt(),
            post.getEditedAt(),
            post.getThread().getId(),
            post.getThread().getTitle(),
            post.getThread().getIsLocked(),
            post.getThread().getCategory().getId(),
            post.getThread().getCategory().getName(),
            pageNumber,
            positionOnPage
        );
    }
}