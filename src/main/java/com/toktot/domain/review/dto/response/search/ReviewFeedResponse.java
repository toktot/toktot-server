package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.Review;
import com.toktot.domain.review.type.MealTime;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
public record ReviewFeedResponse(
        Long id,
        ReviewAuthorResponse author,
        Boolean isBookmarked,
        Boolean isWriter,
        Integer satisfactionScore,
        MealTime mealTime,
        LocalDateTime createdAt,
        Set<String> keywords,
        List<ReviewFeedImageResponse> images,
        ReviewRestaurantInfo restaurant
) {

    public static ReviewFeedResponse from(Review review, ReviewAuthorResponse author,
                                          Set<String> keywords, ReviewRestaurantInfo restaurant,
                                          Boolean isBookmarked, Boolean isWriter) {
        List<ReviewFeedImageResponse> images = review.getImages().stream()
                .map(ReviewFeedImageResponse::from)
                .toList();

        return ReviewFeedResponse.builder()
                .id(review.getId())
                .author(author)
                .isBookmarked(isBookmarked)
                .isWriter(isWriter)
                .satisfactionScore(review.getValueForMoneyScore())
                .mealTime(review.getMealTime())
                .createdAt(review.getCreatedAt())
                .keywords(keywords)
                .images(images)
                .restaurant(restaurant)
                .build();
    }
}
