package com.toktot.domain.review.dto.response;

import com.toktot.domain.review.Tooltip;
import lombok.Builder;

import java.util.List;
import java.util.Optional;

@Builder
public record ReviewMenuResponse(
        String menuName,
        Integer totalPrice
) {

    public static List<ReviewMenuResponse> from(List<Tooltip> tooltips) {
        return Optional.ofNullable(tooltips)
                .orElse(List.of())
                .stream()
                .filter(Tooltip::isFood)
                .map(ReviewMenuResponse::from)
                .toList();
    }


    private static ReviewMenuResponse from(Tooltip tooltip) {
        return ReviewMenuResponse.builder()
                .menuName(tooltip.getMenuName())
                .totalPrice(tooltip.getTotalPrice())
                .build();
    }
}
