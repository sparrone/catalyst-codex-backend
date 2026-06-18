package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.model.FirestorePost;
import com.separrone.awakeningbackend.repository.FirestorePostRepository;
import com.separrone.awakeningbackend.security.FirebaseAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/forum/posts")
public class PostController {

    @Autowired
    private FirestorePostRepository postRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable String id) {
        try {
            Optional<FirestorePost> postOpt = postRepository.findById(id);
            if (postOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(postOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get post: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(httpRequest);

            String content = (String) request.get("content");
            String threadId = (String) request.get("threadId");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
            }
            if (threadId == null || threadId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thread ID is required"));
            }

            FirestorePost post = new FirestorePost(content, threadId, firebaseUid);
            postRepository.save(post);

            return ResponseEntity.status(HttpStatus.CREATED).body(post);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create post: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable String id, @RequestBody Map<String, Object> request, 
                                       HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(httpRequest);

            Optional<FirestorePost> postOpt = postRepository.findById(id);
            if (postOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FirestorePost post = postOpt.get();
            if (!post.getAuthorId().equals(firebaseUid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only edit your own posts"));
            }

            String content = (String) request.get("content");
            if (content != null && !content.trim().isEmpty()) {
                post.setContent(content);
            }

            postRepository.save(post);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update post: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable String id, HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(httpRequest);

            Optional<FirestorePost> postOpt = postRepository.findById(id);
            if (postOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FirestorePost post = postOpt.get();
            if (!post.getAuthorId().equals(firebaseUid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete your own posts"));
            }

            postRepository.delete(id);
            return ResponseEntity.ok(Map.of("message", "Post deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete post: " + e.getMessage()));
        }
    }
}