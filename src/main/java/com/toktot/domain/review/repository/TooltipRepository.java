package com.toktot.domain.review.repository;

import com.toktot.domain.review.Tooltip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TooltipRepository extends JpaRepository<Tooltip, Long> {
    @Query("SELECT t.reviewImage.review.id, AVG(t.rating) FROM Tooltip t WHERE t.reviewImage.review.id IN :reviewIds GROUP BY t.reviewImage.review.id")
    List<Object[]> findAverageRatingsByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    @Query("SELECT t.reviewImage.review.user.id, AVG(t.rating) FROM Tooltip t WHERE t.reviewImage.review.user.id IN :userIds GROUP BY t.reviewImage.review.user.id")
    List<Object[]> findAverageRatingsByUserIds(@Param("userIds") Set<Long> userIds);

    @Query("""
    SELECT t FROM Tooltip t 
    WHERE t.tooltipType = 'FOOD' 
    AND t.menuName IS NOT NULL 
    AND t.totalPrice IS NOT NULL 
    AND t.servingSize IS NOT NULL 
    AND t.servingSize > 0
    ORDER BY t.createdAt DESC
    """)
    List<Tooltip> findAllFoodTooltipsWithPriceData();

    @Query("""
    SELECT DISTINCT t FROM Tooltip t 
    JOIN t.reviewImage ri 
    JOIN ri.review r 
    WHERE t.tooltipType = 'FOOD' 
    AND t.menuName IS NOT NULL 
    AND t.totalPrice IS NOT NULL 
    AND t.servingSize IS NOT NULL 
    AND t.servingSize > 0
    AND (t.totalPrice / t.servingSize) BETWEEN :minPrice AND :maxPrice
    """)
    List<Tooltip> findFoodTooltipsByPriceRange(@Param("minPrice") Integer minPrice,
                                               @Param("maxPrice") Integer maxPrice);
}
