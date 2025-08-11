package com.toktot.web.dto.review.response;

import com.toktot.domain.review.ReviewImage;
import lombok.Builder;

import java.util.List;

@Builder
public record ReviewImageResponse(
        Long id,
        String imageUrl,
        List<ReviewMenuResponse> menus
) {

    public static ReviewImageResponse from(ReviewImage reviewImage) {
        return ReviewImageResponse.builder()
                .id(reviewImage.getId())
                .imageUrl(reviewImage.getImageUrl())
                .menus(ReviewMenuResponse.from(reviewImage.getTooltips()))
                .build();
    }
}
