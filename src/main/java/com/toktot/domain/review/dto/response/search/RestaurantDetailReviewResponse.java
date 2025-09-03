package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.Review;
import com.toktot.domain.review.type.MealTime;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
public record RestaurantDetailReviewResponse(
        Long id,
        ReviewAuthorResponse author,
        BigDecimal reviewRating,
        MealTime mealTime,
        LocalDateTime createdAt,
        Integer satisfactionScore,
        Set<String> keywords,
        Set<ReviewImageDetailResponse> images,
        Set<TooltipResponse> tooltips,
        Boolean isBookmarked,
        Boolean isWriter
) {

    public static RestaurantDetailReviewResponse from(Review review, ReviewAuthorResponse author,
                                                      BigDecimal reviewRating, Set<String> keywords,
                                                      Boolean isBookmarked, Boolean isWriter) {
        Set<ReviewImageDetailResponse> images = review.getImages().stream()
                .map(ReviewImageDetailResponse::from)
                .collect(Collectors.toSet());

        Set<TooltipResponse> allTooltips = review.getImages().stream()
                .flatMap(image -> image.getTooltips().stream())
                .map(TooltipResponse::from)
                .collect(Collectors.toSet());

        return RestaurantDetailReviewResponse.builder()
                .id(review.getId())
                .author(author)
                .reviewRating(reviewRating)
                .mealTime(review.getMealTime())
                .createdAt(review.getCreatedAt())
                .satisfactionScore(review.getValueForMoneyScore())
                .keywords(keywords)
                .images(images)
                .tooltips(allTooltips)
                .isBookmarked(isBookmarked)
                .isWriter(isWriter)
                .build();
    }
}
