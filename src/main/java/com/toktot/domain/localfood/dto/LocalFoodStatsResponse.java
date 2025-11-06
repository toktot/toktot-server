package com.toktot.domain.localfood.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.localfood.LocalFoodType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record LocalFoodStatsResponse(
        @JsonProperty("local_food_type")
        LocalFoodType localFoodType,

        @JsonProperty("display_name")
        String displayName,

        @JsonProperty("total_review_count")
        int totalReviewCount,

        @JsonProperty("average_price")
        int averagePrice,

        @JsonProperty("min_price")
        int minPrice,

        @JsonProperty("max_price")
        int maxPrice,

        @JsonProperty("price_distribution")
        PriceDistribution priceDistribution,

        @JsonProperty("price_ranges")
        List<PriceRangeData> priceRanges,

        @JsonProperty("last_updated")
        String lastUpdated,

        @JsonProperty("has_sufficient_data")
        boolean hasSufficientData
) {

    public static LocalFoodStatsResponse insufficientData(LocalFoodType localFoodType) {
        return LocalFoodStatsResponse.builder()
                .localFoodType(localFoodType)
                .displayName(localFoodType.getDisplayName())
                .totalReviewCount(0)
                .averagePrice(0)
                .minPrice(0)
                .maxPrice(0)
                .priceDistribution(null)
                .priceRanges(List.of())
                .lastUpdated(LocalDateTime.now().toString())
                .hasSufficientData(false)
                .build();
    }

    @Builder
    public record PriceDistribution(
            @JsonProperty("cheap_count")
            int cheapCount,

            @JsonProperty("normal_count")
            int normalCount,

            @JsonProperty("expensive_count")
            int expensiveCount,

            @JsonProperty("cheap_ratio")
            double cheapRatio,

            @JsonProperty("normal_ratio")
            double normalRatio,

            @JsonProperty("expensive_ratio")
            double expensiveRatio
    ) {}

    @Builder
    public record PriceRangeData(
            @JsonProperty("min_price")
            int minPrice,

            @JsonProperty("max_price")
            int maxPrice,

            @JsonProperty("review_count")
            int reviewCount,

            String label
    ) {}
}
