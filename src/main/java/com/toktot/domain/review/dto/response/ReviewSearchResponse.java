package com.toktot.domain.review.dto.response;

import java.time.LocalDateTime;

public record ReviewSearchResponse(
        Long id,
        String firstImageUrl,
        String userNickname,
        String userProfileImageUrl,
        LocalDateTime createdAt,
        String restaurantLocationSummary,
        Double distanceFromUser
) { }
