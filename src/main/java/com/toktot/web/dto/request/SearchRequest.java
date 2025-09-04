package com.toktot.web.dto.request;

import com.toktot.domain.review.type.MealTime;
import com.toktot.domain.search.type.SortType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SearchRequest(
        String query,
        LocationFilterRequest location,
        RatingFilterRequest rating,
        LocalFoodFilterRequest localFood,
        List<String> keywords,
        MealTime mealTime,
        SortType sort
) {

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

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

    public boolean hasSortFilter() { return sort != null; }
}
