package com.toktot.domain.restaurant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.restaurant.Restaurant;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RestaurantDetailResponse(
        Long id,
        String name,
        String address,
        @JsonProperty("is_local_store") Boolean isLocalStore,
        @JsonProperty("value_for_money_score") Integer valueForMoneyScore,
        Integer point,
        @JsonProperty("business_hours") String businessHours,
        String phone,
        String image,
        BigDecimal latitude,
        BigDecimal longitude,
        @JsonProperty("value_for_money_point") Integer valueForMoneyPoint,
        @JsonProperty("percent") String percent
) {

    public static RestaurantDetailResponse from(Restaurant restaurant) {
        return RestaurantDetailResponse
                .builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .phone(restaurant.getPhone())
                .image(restaurant.getImage())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .build();
    }
}
