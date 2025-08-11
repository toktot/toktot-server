package com.toktot.web.dto.review.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toktot.domain.review.Review;
import com.toktot.web.dto.user.ReviewUserResponse;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(
        Long id,
        ReviewUserResponse user,
        List<ReviewImageResponse> images,
        List<String> keywords,
        LocalDateTime createdAt,
        Boolean isBookmarked,
        Boolean isWriter
) {

    public static ReviewResponse from(Review review, Long loginUserId, ReviewUserResponse userResponse) {
        return ReviewResponse.builder()
                .id(review.getId())
                .user(userResponse)
                .images(review.getImages()
                        .stream()
                        .map(ReviewImageResponse::from)
                        .toList())
                .keywords(review.getKeywords()
                        .stream()
                        .map(data -> data.getKeywordType().getDisplayName())
                        .toList())
                .createdAt(review.getCreatedAt())
                .isWriter(review.isWriter(loginUserId))
                .build();
    }

}
