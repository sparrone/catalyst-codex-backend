package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.dto.PostDTO;
import com.separrone.awakeningbackend.dto.ThreadDTO;
import com.separrone.awakeningbackend.dto.ThreadPostsResponseDTO;
import com.separrone.awakeningbackend.entity.Category;
import com.separrone.awakeningbackend.entity.Post;
import com.separrone.awakeningbackend.entity.Thread;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.repository.CategoryRepository;
import com.separrone.awakeningbackend.repository.PostRepository;
import com.separrone.awakeningbackend.repository.ThreadRepository;
import com.separrone.awakeningbackend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/forum/threads")
public class ThreadController {

    private final ThreadRepository threadRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public ThreadController(ThreadRepository threadRepository, CategoryRepository categoryRepository,
                            PostRepository postRepository, UserRepository userRepository) {
        this.threadRepository = threadRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // Get specific thread by ID
    @GetMapping("/{id}")
    public ResponseEntity<ThreadDTO> getThreadById(@PathVariable Long id) {
        return threadRepository.findById(id)
                .map(ThreadDTO::detailFromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get posts in a thread with pagination
    @GetMapping("/{id}/posts")
    public ResponseEntity<ThreadPostsResponseDTO> getPostsInThread(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Thread thread = threadRepository.findById(id).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findByThreadOrderByCreatedAtAsc(thread, pageable);

        List<PostDTO> postDTOs = postsPage.getContent().stream()
                .map(PostDTO::fromEntity)
                .collect(Collectors.toList());

        ThreadPostsResponseDTO response = new ThreadPostsResponseDTO(
                ThreadDTO.detailFromEntity(thread),
                postDTOs,
                postsPage.getNumber(),
                postsPage.getTotalPages(),
                postsPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Create new thread (authenticated users only)
    @PostMapping
    public ResponseEntity<?> createThread(@RequestBody Map<String, Object> request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        String title = (String) request.get("title");
        String content = (String) request.get("content");
        Long categoryId = Long.valueOf(request.get("categoryId").toString());

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Thread title is required");
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Thread content is required");
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return ResponseEntity.badRequest().body("Invalid category");
        }

        // Create the thread
        Thread thread = new Thread(title, category, currentUser);
        Thread savedThread = threadRepository.save(thread);

        // Create the first post
        Post firstPost = new Post(savedThread, currentUser, content);
        postRepository.save(firstPost);

        return ResponseEntity.ok(savedThread);
    }

    // Update thread (original author or admin/moderator only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateThread(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        Thread thread = threadRepository.findById(id).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if user can edit (author or admin/moderator)
        if (!thread.getCreatedBy().getId().equals(currentUser.getId()) && !isAdminOrModerator(currentUser)) {
            return ResponseEntity.status(403).body("Not authorized to edit this thread");
        }

        String title = (String) request.get("title");
        if (title != null && !title.trim().isEmpty()) {
            thread.setTitle(title);
        }

        Thread savedThread = threadRepository.save(thread);
        return ResponseEntity.ok(savedThread);
    }

    // Pin/unpin thread (admin/moderator only)
    @PutMapping("/{id}/pin")
    public ResponseEntity<?> togglePinThread(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null || !isAdminOrModerator(currentUser)) {
            return ResponseEntity.status(403).body("Admin/moderator access required");
        }

        Thread thread = threadRepository.findById(id).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }

        thread.setIsPinned(!thread.getIsPinned());
        Thread savedThread = threadRepository.save(thread);

        return ResponseEntity.ok(Map.of("pinned", savedThread.getIsPinned()));
    }

    // Lock/unlock thread (admin/moderator only)
    @PutMapping("/{id}/lock")
    public ResponseEntity<?> toggleLockThread(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null || !isAdminOrModerator(currentUser)) {
            return ResponseEntity.status(403).body("Admin/moderator access required");
        }

        Thread thread = threadRepository.findById(id).orElse(null);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }

        thread.setIsLocked(!thread.getIsLocked());
        Thread savedThread = threadRepository.save(thread);

        return ResponseEntity.ok(Map.of("locked", savedThread.getIsLocked()));
    }

    // Delete thread (admin/moderator only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteThread(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null || !isAdminOrModerator(currentUser)) {
            return ResponseEntity.status(403).body("Admin/moderator access required");
        }

        if (!threadRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        threadRepository.deleteById(id);
        return ResponseEntity.ok("Thread deleted successfully");
    }

    // Helper methods
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private boolean isAdminOrModerator(User user) {
        // TODO: Implement role checking - for now return false
        // You'll need to implement user roles system
        return false;
    }
}