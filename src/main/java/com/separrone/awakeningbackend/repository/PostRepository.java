package com.separrone.awakeningbackend.repository;

import com.separrone.awakeningbackend.entity.Post;
import com.separrone.awakeningbackend.entity.Thread;
import com.separrone.awakeningbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Get posts in a thread with pagination, ordered chronologically
    Page<Post> findByThreadOrderByCreatedAtAsc(Thread thread, Pageable pageable);

    // Get all posts by a specific user
    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Get the first post of a thread (original post)
    @Query("SELECT p FROM Post p WHERE p.thread = :thread ORDER BY p.createdAt ASC LIMIT 1")
    Post findFirstPostByThread(@Param("thread") Thread thread);

    // Get the latest post of a thread
    @Query("SELECT p FROM Post p WHERE p.thread = :thread ORDER BY p.createdAt DESC LIMIT 1")
    Post findLatestPostByThread(@Param("thread") Thread thread);

    // Count posts in a thread
    long countByThread(Thread thread);

    // Count posts by a user
    long countByUser(User user);

    // Search posts by content (case insensitive)
    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByContentContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Get recent posts across all threads (for activity feed)
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    Page<Post> findRecentPosts(Pageable pageable);

    // Get posts created after a certain date (for "new posts" functionality)
    @Query("SELECT p FROM Post p WHERE p.thread = :thread AND p.createdAt > :since ORDER BY p.createdAt ASC")
    List<Post> findByThreadAndCreatedAtAfter(@Param("thread") Thread thread, @Param("since") LocalDateTime since);

    // Get user's post count for a specific thread
    long countByThreadAndUser(Thread thread, User user);

    // Count posts in a thread that were created before or at the same time as a given date
    long countByThreadAndCreatedAtLessThanEqual(Thread thread, LocalDateTime createdAt);
}