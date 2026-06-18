package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.model.FirestoreThread;
import com.separrone.awakeningbackend.model.FirestorePost;
import com.separrone.awakeningbackend.repository.FirestoreThreadRepository;
import com.separrone.awakeningbackend.repository.FirestorePostRepository;
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
@RequestMapping("/forum/threads")
public class ThreadController {

    @Autowired
    private FirestoreThreadRepository threadRepository;

    @Autowired
    private FirestorePostRepository postRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getThreadById(@PathVariable String id) {
        try {
            Optional<FirestoreThread> threadOpt = threadRepository.findById(id);
            if (threadOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(threadOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get thread: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<?> getThreadPosts(@PathVariable String id) {
        try {
            Optional<FirestoreThread> threadOpt = threadRepository.findById(id);
            if (threadOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<FirestorePost> posts = postRepository.findByThreadId(id);
            
            return ResponseEntity.ok(Map.of(
                    "thread", threadOpt.get(),
                    "posts", posts
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get thread posts: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createThread(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            FirebaseAuthUtil.requireAuthentication(httpRequest);
            String firebaseUid = FirebaseAuthUtil.getFirebaseUid(httpRequest);

            String title = request.get("title");
            String categoryId = request.get("categoryId");

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thread title is required"));
            }
            if (categoryId == null || categoryId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Category ID is required"));
            }

            FirestoreThread thread = new FirestoreThread(title, categoryId, firebaseUid);
            threadRepository.save(thread);

            return ResponseEntity.status(HttpStatus.CREATED).body(thread);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create thread: " + e.getMessage()));
        }
    }
}