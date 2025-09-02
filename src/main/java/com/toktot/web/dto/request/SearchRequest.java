package com.toktot.web.dto.request;

import com.toktot.domain.review.type.MealTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SearchRequest(
        @NotBlank(message = "검색어는 필수입니다.")
        String query,

        @Valid
        LocationFilterRequest location,

        @Valid
        RatingFilterRequest rating,

        @Valid
        LocalFoodFilterRequest localFood,

        List<String> keywords,

        MealTime mealTime
) {
    public boolean hasLocationFilter() {
        return location != null && location.latitude() != null && location.longitude() != null;
    }

    public boolean hasRatingFilter() {
        return rating != null && rating.min() != null;
    }

    public boolean hasLocalFoodFilter() {
        return localFood != null && localFood.type() != null;
    }

    public boolean hasPriceRangeFilter() {
        return localFood != null &&
                (localFood.minPrice() != null || localFood.maxPrice() != null);
    }

    public boolean hasKeywordFilter() {
        return keywords != null && !keywords.isEmpty();
    }
}
