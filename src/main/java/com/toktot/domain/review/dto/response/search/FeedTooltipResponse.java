package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.Tooltip;
import com.toktot.domain.review.type.TooltipType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FeedTooltipResponse(
        Long id,
        BigDecimal xPosition,
        BigDecimal yPosition,
        TooltipType type,
        BigDecimal rating,
        String menuName,
        Integer totalPrice,
        Integer servingSize,
        String detailedReview
) {

    public static FeedTooltipResponse from(Tooltip tooltip) {
        return FeedTooltipResponse.builder()
                .id(tooltip.getId())
                .xPosition(tooltip.getXPosition())
                .yPosition(tooltip.getYPosition())
                .type(tooltip.getTooltipType())
                .rating(tooltip.getRating())
                .menuName(tooltip.getMenuName())
                .totalPrice(tooltip.getTotalPrice())
                .servingSize(tooltip.getServingSize())
                .detailedReview(tooltip.getDetailedReview())
                .build();
    }
}
