package com.separrone.awakeningbackend.controller;

import com.separrone.awakeningbackend.dto.UserPostDTO;
import com.separrone.awakeningbackend.entity.Post;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.repository.PostRepository;
import com.separrone.awakeningbackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class PublicUserController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public PublicUserController(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getPublicProfile(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(Map.of(
                        "username", user.getUsername(),
                        "createdAt", user.getCreatedAt()
                        // Add other SAFE public fields here in the future
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{username}/posts")
    public ResponseEntity<Map<String, Object>> getUserPosts(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        List<UserPostDTO> userPosts = new ArrayList<>();
        
        for (Post post : postsPage.getContent()) {
            // Calculate which page this post appears on in its thread (for deep linking)
            // Assuming 10 posts per page in threads (matching ThreadController default)
            int postsPerPage = 10;
            
            // Get all posts in this thread that were created before or at the same time as this post
            long postsBeforeThis = postRepository.countByThreadAndCreatedAtLessThanEqual(
                post.getThread(), post.getCreatedAt());
            
            // Calculate page number (0-based) and position on page (1-based)
            int pageNumber = (int) ((postsBeforeThis - 1) / postsPerPage);
            int positionOnPage = (int) ((postsBeforeThis - 1) % postsPerPage) + 1;
            
            userPosts.add(UserPostDTO.fromEntity(post, pageNumber, positionOnPage));
        }
        
        Map<String, Object> response = Map.of(
            "posts", userPosts,
            "currentPage", postsPage.getNumber(),
            "totalPages", postsPage.getTotalPages(),
            "totalElements", postsPage.getTotalElements(),
            "hasNext", postsPage.hasNext(),
            "hasPrevious", postsPage.hasPrevious()
        );
        
        return ResponseEntity.ok(response);
    }
}