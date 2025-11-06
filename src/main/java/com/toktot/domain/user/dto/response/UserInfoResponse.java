package com.toktot.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserInfoResponse(
        String nickname,
        @JsonProperty("review_count") Long reviewCount,
        @JsonProperty("review_rating_avg") Double reviewRatingAvg,
        @JsonProperty("profile_image_url") String profileImageUrl
) {
}
