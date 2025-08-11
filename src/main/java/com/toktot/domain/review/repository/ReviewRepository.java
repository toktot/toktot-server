package com.toktot.domain.review.repository;

import com.toktot.domain.review.Review;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
    SELECT DISTINCT r FROM Review r
    JOIN FETCH r.user u
    LEFT JOIN FETCH r.images i
    LEFT JOIN FETCH i.tooltips t
    WHERE r.restaurant.id = :restaurantId
    """)
    Page<Review> findByRestaurantIdWithDetails(@Param("restaurantId") Long restaurantId, Pageable pageable);
}
