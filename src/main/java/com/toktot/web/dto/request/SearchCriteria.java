package com.toktot.web.dto.request;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.review.type.MealTime;

import java.util.List;

public record SearchCriteria(
        String query,
        Double latitude,
        Double longitude,
        Integer radius,
        Double minRating,
        LocalFoodType localFoodType,
        Integer localFoodMinPrice,
        Integer localFoodMaxPrice,
        MealTime mealTime,
        List<String> keywords
) {
    public static SearchCriteria from(SearchRequest request) {
        return new SearchCriteria(
                request.query().trim(),
                request.hasLocationFilter() ? request.location().latitude() : null,
                request.hasLocationFilter() ? request.location().longitude() : null,
                request.hasLocationFilter() ? request.location().radius() : null,
                request.hasRatingFilter() ? request.rating().getMinAsDouble() : null,
                request.hasLocalFoodFilter() ? request.localFood().type() : null,
                request.hasPriceRangeFilter() ? request.localFood().minPrice() : null,
                request.hasPriceRangeFilter() ? request.localFood().maxPrice() : null,
                request.mealTime(),
                request.keywords()
        );
    }

    public boolean hasLocationFilter() {
        return latitude != null && longitude != null && radius != null;
    }

    public boolean hasRatingFilter() {
        return minRating != null;
    }

    public boolean hasLocalFoodFilter() {
        return localFoodType != null;
    }

    public boolean hasPriceRangeFilter() {
        return localFoodMinPrice != null || localFoodMaxPrice != null;
    }

    public boolean hasKeywordFilter() {
        return keywords != null && !keywords.isEmpty();
    }

    public boolean hasMealTimeFilter() {
        return mealTime != null;
    }

    public boolean hasValidQuery() {
        return query != null && !query.trim().isEmpty();
    }
}

