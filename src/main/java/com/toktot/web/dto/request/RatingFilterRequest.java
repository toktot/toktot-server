package com.toktot.web.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record RatingFilterRequest(
        @DecimalMin(value = "0.5", message = "최소 별점은 0.5입니다.")
        @DecimalMax(value = "5.0", message = "최대 별점은 5.0입니다.")
        BigDecimal min
) {
    public boolean isValid() {
        if (min == null) {
            return true;
        }

        return min.compareTo(new BigDecimal("0.5")) >= 0 &&
                min.compareTo(new BigDecimal("5.0")) <= 0;
    }

    public BigDecimal getNormalizedRating() {
        if (min == null) {
            return null;
        }

        double value = min.doubleValue();
        double normalized = Math.round(value * 2.0) / 2.0;

        if (normalized < 0.5) normalized = 0.5;
        if (normalized > 5.0) normalized = 5.0;

        return BigDecimal.valueOf(normalized);
    }

    public Double getMinAsDouble() {
        return min != null ? min.doubleValue() : null;
    }
}
