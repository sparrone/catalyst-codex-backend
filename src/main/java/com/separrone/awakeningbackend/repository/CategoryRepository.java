package com.separrone.awakeningbackend.repository;

import com.separrone.awakeningbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Get all categories ordered by sort order
    List<Category> findAllByOrderBySortOrderAsc();

    // Check if category name already exists (for validation)
    boolean existsByName(String name);

    // Get category by name
    Category findByName(String name);

    // Get categories with thread count (for displaying forum stats)
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.threads t ORDER BY c.sortOrder ASC")
    List<Category> findAllWithThreads();
}