package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.ReviewImage;
import lombok.Builder;

import java.util.List;

@Builder
public record ReviewImageDetailResponse(
        String imageId,
        String imageUrl,
        Integer imageOrder,
        Boolean isMain,
        List<TooltipResponse> tooltips
) {

    public static ReviewImageDetailResponse from(ReviewImage reviewImage) {
        List<TooltipResponse> tooltips = reviewImage.getTooltips().stream()
                .map(TooltipResponse::from)
                .toList();

        return ReviewImageDetailResponse.builder()
                .imageId(reviewImage.getImageId())
                .imageUrl(reviewImage.getImageUrl())
                .imageOrder(reviewImage.getImageOrder())
                .isMain(reviewImage.getIsMain())
                .tooltips(tooltips)
                .build();
    }
}
