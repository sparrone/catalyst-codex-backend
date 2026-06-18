package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.model.FirestoreCategory;
import com.separrone.awakeningbackend.model.FirestoreThread;
import com.separrone.awakeningbackend.repository.FirestoreCategoryRepository;
import com.separrone.awakeningbackend.repository.FirestoreThreadRepository;
import com.separrone.awakeningbackend.security.FirebaseAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/forum/categories")
public class CategoryController {

    @Autowired
    private FirestoreCategoryRepository categoryRepository;

    @Autowired
    private FirestoreThreadRepository threadRepository;

    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<FirestoreCategory> categories = categoryRepository.findAll();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get categories: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable String id) {
        try {
            Optional<FirestoreCategory> categoryOpt = categoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(categoryOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get category: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/threads")
    public ResponseEntity<?> getCategoryThreads(@PathVariable String id) {
        try {
            Optional<FirestoreCategory> categoryOpt = categoryRepository.findById(id);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<FirestoreThread> threads = threadRepository.findByCategoryId(id);
            
            return ResponseEntity.ok(Map.of(
                    "category", categoryOpt.get(),
                    "threads", threads
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get category threads: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);

            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Category name is required"));
            }

            FirestoreCategory category = new FirestoreCategory(name, description);
            categoryRepository.save(category);

            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create category: " + e.getMessage()));
        }
    }
}