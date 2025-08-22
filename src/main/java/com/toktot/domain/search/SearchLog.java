package com.toktot.domain.search;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.review.type.MealTime;
import com.toktot.domain.search.type.SearchTab;
import com.toktot.domain.search.type.SortType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 200)
    private String keyword;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Builder.Default
    @Column(nullable = false)
    private Integer radius = 1000;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "search_tab", nullable = false, length = 20)
    private SearchTab searchTab = SearchTab.RESTAURANTS;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filter_keywords", columnDefinition = "json")
    private String filterKeywords;

    @Builder.Default
    @Column(name = "filter_good_price", nullable = false)
    private Boolean filterGoodPrice = false;

    @Column(name = "filter_min_rating", precision = 2, scale = 1)
    private BigDecimal filterMinRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_local_food_type", length = 50)
    private LocalFoodType filterLocalFoodType;

    @Column(name = "filter_min_price")
    private Integer filterMinPrice;

    @Column(name = "filter_max_price")
    private Integer filterMaxPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_meal_time", length = 20)
    private MealTime filterMealTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sort_type", length = 20)
    private SortType sortType = SortType.DISTANCE;

    @Builder.Default
    @Column(name = "result_count", nullable = false)
    private Integer resultCount = 0;

    @Builder.Default
    @Column(name = "response_time_ms", nullable = false)
    private Integer responseTimeMs = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
