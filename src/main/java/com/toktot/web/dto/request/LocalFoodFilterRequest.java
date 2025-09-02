package com.toktot.web.dto.request;

import com.toktot.domain.localfood.LocalFoodType;
import jakarta.validation.constraints.Min;

public record LocalFoodFilterRequest(
        LocalFoodType type,

        @Min(value = 0, message = "최소 가격은 0원 이상이어야 합니다.")
        Integer minPrice,

        @Min(value = 0, message = "최대 가격은 0원 이상이어야 합니다.")
        Integer maxPrice
) {
    public boolean hasType() {
        return type != null;
    }

    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }

    public boolean isPriceRangeValid() {
        if (minPrice == null && maxPrice == null) {
            return true;
        }

        if (minPrice != null && maxPrice != null) {
            return minPrice <= maxPrice;
        }

        return true;
    }

    public String getPriceRangeErrorMessage() {
        if (!isPriceRangeValid()) {
            return "최소 가격이 최대 가격보다 클 수 없습니다.";
        }

        if (!hasType() && hasPriceRange()) {
            return "가격 필터는 향토음식 타입과 함께 사용해야 합니다.";
        }

        return null;
    }

    public boolean isValid() {
        return isPriceRangeValid() &&
                (hasType() || !hasPriceRange());
    }
}
