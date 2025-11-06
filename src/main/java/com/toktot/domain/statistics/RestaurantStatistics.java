package com.toktot.domain.statistics;

import com.toktot.domain.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "restaurant_statistics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantStatistics {

    @Id
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating;

    @Column(name = "average_satisfaction")
    private Integer averageSatisfaction;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    private Integer clickCount = 0;

    @Builder.Default
    @Column(name = "bookmark_count", nullable = false)
    private Integer bookmarkCount = 0;

    @Builder.Default
    @Column(name = "has_local_food", nullable = false)
    private Boolean hasLocalFood = false;

    @Builder.Default
    @Column(name = "local_food_review_count")
    private Integer localFoodReviewCount = 0;

    @Column(name = "local_food_avg_price")
    private Integer localFoodAvgPrice;

    @Column(name = "local_food_min_price")
    private Integer localFoodMinPrice;

    @Column(name = "local_food_max_price")
    private Integer localFoodMaxPrice;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
