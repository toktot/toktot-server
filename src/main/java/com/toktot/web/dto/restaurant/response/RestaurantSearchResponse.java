package com.toktot.web.dto.restaurant.response;

import com.toktot.domain.restaurant.Restaurant;

public record RestaurantSearchResponse(
        Long id,
        String externalTourApiId,
        String externalKakaoId,
        String name
) {

    public static RestaurantSearchResponse from(Restaurant entity) {
        return new RestaurantSearchResponse(
                entity.getId(),
                entity.getExternalTourApiId(),
                entity.getExternalKakaoId(),
                entity.getName()
        );
    }
}
