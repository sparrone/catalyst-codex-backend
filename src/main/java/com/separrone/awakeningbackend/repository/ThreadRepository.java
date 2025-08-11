package com.separrone.awakeningbackend.repository;

import com.separrone.awakeningbackend.entity.Thread;
import com.separrone.awakeningbackend.entity.Category;
import com.separrone.awakeningbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {

    // Get threads in a category with pagination, ordered by pinned first, then by last post
    @Query("SELECT t FROM Thread t WHERE t.category = :category ORDER BY t.isPinned DESC, t.lastPostAt DESC")
    Page<Thread> findByCategoryOrderByPinnedAndLastPost(@Param("category") Category category, Pageable pageable);

    // Get all threads by a specific user
    Page<Thread> findByCreatedByOrderByCreatedAtDesc(User user, Pageable pageable);

    // Search threads by title (case insensitive)
    @Query("SELECT t FROM Thread t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.lastPostAt DESC")
    Page<Thread> findByTitleContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Get pinned threads in a category
    List<Thread> findByCategoryAndIsPinnedTrueOrderByLastPostAtDesc(Category category);

    // Get recent threads across all categories (for homepage or recent activity)
    @Query("SELECT t FROM Thread t ORDER BY t.lastPostAt DESC")
    Page<Thread> findRecentThreads(Pageable pageable);

    // Count threads in a category
    long countByCategory(Category category);

    // Get threads with their first post (for thread previews)
    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.posts p WHERE t.category = :category AND (p.id = (SELECT MIN(p2.id) FROM Post p2 WHERE p2.thread = t) OR p IS NULL) ORDER BY t.isPinned DESC, t.lastPostAt DESC")
    Page<Thread> findByCategoryWithFirstPost(@Param("category") Category category, Pageable pageable);
}