package com.toktot.web.dto.review.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewCreateResponse(
        @JsonProperty("review_id")
        Long reviewId,

        @JsonProperty("restaurant_id")
        Long restaurantId
) {

    public static ReviewCreateResponse from(Long reviewId, Long restaurantId) {
        return new ReviewCreateResponse(reviewId, restaurantId);
    }
}
