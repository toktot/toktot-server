package com.toktot.domain.review.repository;

import com.toktot.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
        SELECT DISTINCT r FROM Review r 
        JOIN FETCH r.user 
        LEFT JOIN FETCH r.images 
        LEFT JOIN FETCH r.keywords 
        WHERE r.id IN :ids
        """)
    List<Review> findWithDetailsByIds(@Param("ids") List<Long> ids);

    @Query("SELECT r.user.id, COUNT(r) FROM Review r WHERE r.user.id IN :userIds GROUP BY r.user.id")
    List<Object[]> findReviewCountsByUserIds(@Param("userIds") Set<Long> userIds);
}
