package com.toktot.domain.user.dto.response;

import com.toktot.domain.user.User;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReviewUserResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        Integer reviewCount,
        BigDecimal averageRating
) {

    public static ReviewUserResponse from(User user, Integer reviewCount, BigDecimal averageRating) {
        if (averageRating == null) {
            averageRating = BigDecimal.ZERO;
        }

        return ReviewUserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .reviewCount(reviewCount)
                .averageRating(averageRating)
                .build();
    }

}
