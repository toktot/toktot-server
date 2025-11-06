package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.user.User;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReviewAuthorResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        Integer reviewCount,
        BigDecimal averageRating
) {

    public static ReviewAuthorResponse from(User user, Integer reviewCount, BigDecimal averageRating) {
        return ReviewAuthorResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .reviewCount(reviewCount != null ? reviewCount : 0)
                .averageRating(averageRating != null ? averageRating : BigDecimal.ZERO)
                .build();
    }
}
