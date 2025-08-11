package com.separrone.awakeningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreadPostsResponseDTO {
    private ThreadDTO thread;
    private List<PostDTO> posts;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}