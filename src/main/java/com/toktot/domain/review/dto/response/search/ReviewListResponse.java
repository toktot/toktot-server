package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.Review;
import com.toktot.domain.review.ReviewImage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewListResponse(
        Long id,
        String mainImageUrl,
        String authorProfileImageUrl,
        String authorNickname,
        LocalDateTime createdAt,
        ReviewRestaurantInfo restaurant,
        Boolean isBookmarked,
        Boolean isWriter
) {

    public static ReviewListResponse from(Review review, ReviewRestaurantInfo restaurant,
                                          Boolean isBookmarked, Boolean isWriter) {
        String mainImageUrl = review.getImages().stream()
                .filter(ReviewImage::getIsMain)
                .findFirst()
                .map(ReviewImage::getImageUrl)
                .orElse(review.getImages().isEmpty() ? null : review.getImages().stream().toList().getFirst().getImageUrl());

        return ReviewListResponse.builder()
                .id(review.getId())
                .mainImageUrl(mainImageUrl)
                .authorProfileImageUrl(review.getUser().getProfileImageUrl())
                .authorNickname(review.getUser().getNickname())
                .createdAt(review.getCreatedAt())
                .restaurant(restaurant)
                .isBookmarked(isBookmarked)
                .isWriter(isWriter)
                .build();
    }
}
