package com.separrone.awakeningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryThreadsResponseDTO {
    private CategoryDTO category;
    private List<ThreadDTO> threads;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}