package com.toktot.domain.review.dto.response.search;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RestaurantReviewStatisticsResponse(
        Integer totalReviewCount,
        BigDecimal overallRating,
        TooltipTypeRating tooltipRatings,
        SatisfactionScoreDistribution satisfactionDistribution
) {

    @Builder
    public record TooltipTypeRating(
            BigDecimal foodRating,
            BigDecimal cleanRating,
            BigDecimal serviceRating
    ) {}

    @Builder
    public record SatisfactionScoreDistribution(
            Double highRange, // 100~70점 비율
            Double midRange,  // 69~40점 비율
            Double lowRange   // 0~39점 비율
    ) {}

    public static RestaurantReviewStatisticsResponse from(Integer totalReviewCount,
                                                          BigDecimal overallRating,
                                                          BigDecimal foodRating,
                                                          BigDecimal cleanRating,
                                                          BigDecimal serviceRating,
                                                          Double highRange,
                                                          Double midRange,
                                                          Double lowRange) {
        TooltipTypeRating tooltipRatings = TooltipTypeRating.builder()
                .foodRating(foodRating != null ? foodRating : BigDecimal.ZERO)
                .cleanRating(cleanRating != null ? cleanRating : BigDecimal.ZERO)
                .serviceRating(serviceRating != null ? serviceRating : BigDecimal.ZERO)
                .build();

        SatisfactionScoreDistribution satisfactionDistribution = SatisfactionScoreDistribution.builder()
                .highRange(highRange != null ? highRange : 0.0)
                .midRange(midRange != null ? midRange : 0.0)
                .lowRange(lowRange != null ? lowRange : 0.0)
                .build();

        return RestaurantReviewStatisticsResponse.builder()
                .totalReviewCount(totalReviewCount != null ? totalReviewCount : 0)
                .overallRating(overallRating != null ? overallRating : BigDecimal.ZERO)
                .tooltipRatings(tooltipRatings)
                .satisfactionDistribution(satisfactionDistribution)
                .build();
    }
}
