package com.toktot.domain.localfood.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.localfood.LocalFoodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PriceRangeRequest(
        @JsonProperty("local_food_type")
        @NotNull(message = "향토음식 타입은 필수입니다.")
        LocalFoodType localFoodType,

        @JsonProperty("clicked_price")
        @NotNull(message = "가격은 필수입니다.")
        @Positive(message = "가격은 양수여야 합니다.")
        Integer clickedPrice,

        BigDecimal latitude,

        BigDecimal longitude,

        Integer radius
) {
    private static final int PRICE_RANGE_MARGIN = 2500;
    private static final int DEFAULT_RADIUS = 30000;

    public Integer getMinPrice() {
        return Math.max(0, clickedPrice - PRICE_RANGE_MARGIN);
    }

    public Integer getMaxPrice() {
        return clickedPrice + PRICE_RANGE_MARGIN;
    }

    public Integer getRadius() {
        return radius != null ? radius : DEFAULT_RADIUS;
    }
}
