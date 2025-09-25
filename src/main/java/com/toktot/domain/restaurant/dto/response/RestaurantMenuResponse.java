package com.toktot.domain.restaurant.dto.response;

import com.toktot.domain.restaurant.RestaurantMenu;

public record RestaurantMenuResponse (
        Long menuId,
        String menuName,
        String menuImageUrl,
        String category,
        Integer price,
        Integer servingSize,
        Integer pricePerServing,
        Boolean isMain,
        String localFoodType
) {

    public static RestaurantMenuResponse from(RestaurantMenu entity) {
        return new RestaurantMenuResponse(
                entity.getId(),
                entity.getMenuName(),
                entity.getMenuImageUrl(),
                entity.getCategory() != null ? entity.getCategory().getDisplayName() : "기타",
                entity.getPrice(),
                entity.getServingSize(),
                entity.getPricePerServing(),
                entity.getIsMain(),
                entity.getLocalFoodType() != null ? entity.getLocalFoodType().name() : null
        );
    }
}
