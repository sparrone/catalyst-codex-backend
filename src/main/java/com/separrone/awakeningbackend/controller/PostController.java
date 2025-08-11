package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.dto.PostDTO;
import com.separrone.awakeningbackend.entity.Post;
import com.separrone.awakeningbackend.entity.Thread;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.repository.PostRepository;
import com.separrone.awakeningbackend.repository.ThreadRepository;
import com.separrone.awakeningbackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/forum/posts")
public class PostController {

    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;

    public PostController(PostRepository postRepository, ThreadRepository threadRepository,
                          UserRepository userRepository) {
        this.postRepository = postRepository;
        this.threadRepository = threadRepository;
        this.userRepository = userRepository;
    }

    // Get specific post by ID
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(PostDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new post (reply to thread)
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            System.out.println("Current user: " + (currentUser != null ? currentUser.getUsername() : "null"));
            
            if (currentUser == null) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            String content = (String) request.get("content");
            Object threadIdObj = request.get("threadId");
            System.out.println("Request content: " + content);
            System.out.println("Request threadId: " + threadIdObj);

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Post content is required");
            }

            Long threadId;
            try {
                threadId = Long.valueOf(threadIdObj.toString());
            } catch (Exception e) {
                System.out.println("Invalid threadId format: " + threadIdObj);
                return ResponseEntity.badRequest().body("Invalid thread ID format");
            }

            Thread thread = threadRepository.findById(threadId).orElse(null);
            if (thread == null) {
                System.out.println("Thread not found: " + threadId);
                return ResponseEntity.badRequest().body("Invalid thread");
            }

            if (thread.getIsLocked()) {
                return ResponseEntity.status(403).body("Thread is locked");
            }

            // Create the post
            Post post = new Post(thread, currentUser, content);
            Post savedPost = postRepository.save(post);
            System.out.println("Post saved successfully: " + savedPost.getId());

            // Update thread's last post time
            thread.setLastPostAt(LocalDateTime.now());
            threadRepository.save(thread);

            return ResponseEntity.ok(PostDTO.fromEntity(savedPost));
        } catch (Exception e) {
            System.out.println("Error creating post: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    // Update post (original author or admin/moderator only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if user can edit (author or admin/moderator)
        if (!post.getUser().getId().equals(currentUser.getId()) && !isAdminOrModerator(currentUser)) {
            return ResponseEntity.status(403).body("Not authorized to edit this post");
        }

        String content = (String) request.get("content");
        if (content != null && !content.trim().isEmpty()) {
            post.setContent(content);
            post.setEditedAt(LocalDateTime.now());
        }

        Post savedPost = postRepository.save(post);
        return ResponseEntity.ok(savedPost);
    }

    // Delete post (admin/moderator only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null || !isAdminOrModerator(currentUser)) {
            return ResponseEntity.status(403).body("Admin/moderator access required");
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        // Don't allow deleting the first post of a thread (would break thread structure)
        Post firstPost = postRepository.findFirstPostByThread(post.getThread());
        if (firstPost != null && firstPost.getId().equals(post.getId())) {
            return ResponseEntity.badRequest().body("Cannot delete the first post of a thread");
        }

        postRepository.deleteById(id);
        return ResponseEntity.ok("Post deleted successfully");
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