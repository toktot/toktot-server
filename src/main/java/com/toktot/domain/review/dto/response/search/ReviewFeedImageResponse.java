package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.ReviewImage;
import lombok.Builder;

import java.util.List;

@Builder
public record ReviewFeedImageResponse(
        String imageId,
        String imageUrl,
        Integer imageOrder,
        Boolean isMain,
        List<FeedTooltipResponse> tooltips
) {

    public static ReviewFeedImageResponse from(ReviewImage reviewImage) {
        List<FeedTooltipResponse> tooltips = reviewImage.getTooltips().stream()
                .map(FeedTooltipResponse::from)
                .toList();

        return ReviewFeedImageResponse.builder()
                .imageId(reviewImage.getImageId())
                .imageUrl(reviewImage.getImageUrl())
                .imageOrder(reviewImage.getImageOrder())
                .isMain(reviewImage.getIsMain())
                .tooltips(tooltips)
                .build();
    }
}
