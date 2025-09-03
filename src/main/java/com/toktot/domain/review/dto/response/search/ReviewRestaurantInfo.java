package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.restaurant.Restaurant;
import lombok.Builder;

@Builder
public record ReviewRestaurantInfo(
        Long id,
        String name,
        String representativeMenu,
        String address,
        Double distanceInKm
) {

    public static ReviewRestaurantInfo from(Restaurant restaurant, Double distanceInKm) {
        return ReviewRestaurantInfo.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .representativeMenu(restaurant.getPopularMenus())
                .address(extractCityAndDistrict(restaurant.getAddress()))
                .distanceInKm(distanceInKm)
                .build();
    }

    private static String extractCityAndDistrict(String fullAddress) {
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            return null;
        }

        String[] parts = fullAddress.replace("제주특별자치도", "")
                .trim()
                .split("\\s+");

        if (parts.length < 2) {
            return fullAddress;
        }

        return parts[0] + " " + parts[1];
    }
}
