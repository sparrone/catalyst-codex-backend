package com.separrone.awakeningbackend.dto;

import com.separrone.awakeningbackend.entity.Thread;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThreadDTO {
    private Long id;
    private String title;
    private CategoryDTO category;  // Null for summary view, included for detail view
    private UserDTO createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime lastPostAt;
    private Boolean isPinned;
    private Boolean isLocked;
    
    // Factory method for thread summaries (no category)
    public static ThreadDTO summaryFromEntity(Thread thread) {
        return new ThreadDTO(
            thread.getId(),
            thread.getTitle(),
            null,  // No category in summary
            UserDTO.fromEntity(thread.getCreatedBy()),
            thread.getCreatedAt(),
            thread.getLastPostAt(),
            thread.getIsPinned(),
            thread.getIsLocked()
        );
    }
    
    // Factory method for thread details (with category)
    public static ThreadDTO detailFromEntity(Thread thread) {
        return new ThreadDTO(
            thread.getId(),
            thread.getTitle(),
            CategoryDTO.fromEntity(thread.getCategory()),
            UserDTO.fromEntity(thread.getCreatedBy()),
            thread.getCreatedAt(),
            thread.getLastPostAt(),
            thread.getIsPinned(),
            thread.getIsLocked()
        );
    }
}