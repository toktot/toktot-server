package com.toktot.domain.restaurant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PriceRangeRestaurantResponse(
        @JsonProperty("restaurant_id")
        Long restaurantId,

        @JsonProperty("restaurant_name")
        String restaurantName,

        String address,

        BigDecimal latitude,

        BigDecimal longitude,

        Double distance,

        String category,

        @JsonProperty("average_rating")
        Double averageRating,

        @JsonProperty("review_count")
        Integer reviewCount,

        @JsonProperty("is_good_price_store")
        Boolean isGoodPriceStore,

        @JsonProperty("image_url")
        String ImageUrl,

        @JsonProperty("average_price_in_range")
        Integer averagePriceInRange
) {
    public static PriceRangeRestaurantResponse of(
            Long restaurantId,
            String restaurantName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Double distance,
            String category,
            Double averageRating,
            Integer reviewCount,
            Boolean isGoodPriceStore,
            String representativeImageUrl,
            Integer averagePriceInRange
    ) {
        return PriceRangeRestaurantResponse.builder()
                .restaurantId(restaurantId)
                .restaurantName(restaurantName)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .distance(distance)
                .category(category)
                .averageRating(averageRating)
                .reviewCount(reviewCount != null ? reviewCount : 0)
                .isGoodPriceStore(isGoodPriceStore != null && isGoodPriceStore)
                .ImageUrl(representativeImageUrl)
                .averagePriceInRange(averagePriceInRange)
                .build();
    }
}
