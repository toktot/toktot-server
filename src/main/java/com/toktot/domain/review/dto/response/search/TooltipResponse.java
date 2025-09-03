package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.Tooltip;
import com.toktot.domain.review.type.TooltipType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TooltipResponse(
        Long id,
        TooltipType type,
        BigDecimal rating,
        String menuName,
        Integer totalPrice,
        Integer servingSize,
        String detailedReview
) {

    public static TooltipResponse from(Tooltip tooltip) {
        return TooltipResponse.builder()
                .id(tooltip.getId())
                .type(tooltip.getTooltipType())
                .rating(tooltip.getRating())
                .menuName(tooltip.getMenuName())
                .totalPrice(tooltip.getTotalPrice())
                .servingSize(tooltip.getServingSize())
                .detailedReview(tooltip.getDetailedReview())
                .build();
    }
}
