package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.dto.CategoryDTO;
import com.separrone.awakeningbackend.dto.CategoryThreadsResponseDTO;
import com.separrone.awakeningbackend.dto.ThreadDTO;
import com.separrone.awakeningbackend.entity.Category;
import com.separrone.awakeningbackend.entity.Thread;
import com.separrone.awakeningbackend.repository.CategoryRepository;
import com.separrone.awakeningbackend.repository.ThreadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/forum/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ThreadRepository threadRepository;

    public CategoryController(CategoryRepository categoryRepository, ThreadRepository threadRepository) {
        this.categoryRepository = categoryRepository;
        this.threadRepository = threadRepository;
    }

    // Get all categories (for forum homepage)
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        List<CategoryDTO> categoryDTOs = categories.stream()
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categoryDTOs);
    }

    // Get specific category by ID
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(CategoryDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get threads in a category with pagination
    @GetMapping("/{id}/threads")
    public ResponseEntity<CategoryThreadsResponseDTO> getThreadsInCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Thread> threadsPage = threadRepository.findByCategoryOrderByPinnedAndLastPost(category, pageable);

        List<ThreadDTO> threadDTOs = threadsPage.getContent().stream()
                .map(ThreadDTO::summaryFromEntity)
                .collect(Collectors.toList());

        CategoryThreadsResponseDTO response = new CategoryThreadsResponseDTO(
                CategoryDTO.fromEntity(category),
                threadDTOs,
                threadsPage.getNumber(),
                threadsPage.getTotalPages(),
                threadsPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Admin only: Create new category
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Map<String, Object> request) {
        // TODO: Add admin role check here

        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Integer sortOrder = (Integer) request.get("sortOrder");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Category name is required");
        }

        if (categoryRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body("Category name already exists");
        }

        Category category = new Category(name, description, sortOrder);
        Category savedCategory = categoryRepository.save(category);

        return ResponseEntity.ok(savedCategory);
    }

    // Admin only: Update category
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        // TODO: Add admin role check here

        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Integer sortOrder = (Integer) request.get("sortOrder");

        if (name != null) category.setName(name);
        if (description != null) category.setDescription(description);
        if (sortOrder != null) category.setSortOrder(sortOrder);

        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(savedCategory);
    }

    // Admin only: Delete category
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        // TODO: Add admin role check here

        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // Check if category has threads
        long threadCount = threadRepository.countByCategory(categoryRepository.findById(id).get());
        if (threadCount > 0) {
            return ResponseEntity.badRequest().body("Cannot delete category with existing threads");
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Category deleted successfully");
    }
}