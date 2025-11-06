package com.toktot.domain.statistics;

import com.toktot.domain.localfood.LocalFoodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "local_food_statistics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_local_food_type", columnNames = "food_type")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalFoodStatistics {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", length = 50)
    private LocalFoodType foodType;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Builder.Default
    @Column(name = "restaurant_count", nullable = false)
    private Integer restaurantCount = 0;

    @Column(name = "average_price")
    private Integer averagePrice;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "max_price")
    private Integer maxPrice;

    @Builder.Default
    @Column(name = "cheap_count", nullable = false)
    private Integer cheapCount = 0;

    @Builder.Default
    @Column(name = "fair_count", nullable = false)
    private Integer fairCount = 0;

    @Builder.Default
    @Column(name = "expensive_count", nullable = false)
    private Integer expensiveCount = 0;

    @Column(name = "price_ranges", columnDefinition = "json")
    private String priceRanges;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
