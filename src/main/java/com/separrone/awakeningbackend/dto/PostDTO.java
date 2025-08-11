package com.separrone.awakeningbackend.dto;

import com.separrone.awakeningbackend.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private String content;
    private UserDTO user;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    
    public static PostDTO fromEntity(Post post) {
        return new PostDTO(
            post.getId(),
            post.getContent(),
            UserDTO.fromEntity(post.getUser()),
            post.getCreatedAt(),
            post.getEditedAt()
        );
    }
}