package com.toktot.domain.restaurant.dto.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record GoodPriceRestaurantDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("address") String address,
        @JsonProperty("latitude") BigDecimal latitude,
        @JsonProperty("longitude") BigDecimal longitude,
        @JsonProperty("main_menus") String mainMenus,
        @JsonProperty("average_rating") BigDecimal averageRating,
        @JsonProperty("review_count") Long reviewCount,
        @JsonProperty("is_good_price_store") Boolean isGoodPriceStore,
        @JsonProperty("is_local_store") Boolean isLocalStore,
        @JsonProperty("image") String image,
        @JsonProperty("phone") String phone,
        @JsonProperty("average_price") Double averagePrice,
        @JsonProperty("price_range") Integer priceRange,
        @JsonProperty("cached_at") LocalDateTime cachedAt
) {

    public static GoodPriceRestaurantDto from(
            com.toktot.domain.restaurant.Restaurant restaurant,
            Double averagePrice,
            Integer priceRange,
            BigDecimal averageRating,
            Long reviewCount) {

        return GoodPriceRestaurantDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .mainMenus(restaurant.getPopularMenus())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .isGoodPriceStore(restaurant.getIsGoodPriceStore())
                .isLocalStore(restaurant.getIsLocalStore())
                .image(restaurant.getImage())
                .phone(restaurant.getPhone())
                .averagePrice(averagePrice)
                .priceRange(priceRange)
                .cachedAt(LocalDateTime.now())
                .build();
    }

    public static GoodPriceRestaurantDto fromWithoutStats(
            com.toktot.domain.restaurant.Restaurant restaurant,
            Double averagePrice,
            Integer priceRange) {

        return GoodPriceRestaurantDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .mainMenus(restaurant.getPopularMenus())
                .averageRating(BigDecimal.ZERO)
                .reviewCount(0L)
                .isGoodPriceStore(restaurant.getIsGoodPriceStore())
                .isLocalStore(restaurant.getIsLocalStore())
                .image(restaurant.getImage())
                .phone(restaurant.getPhone())
                .averagePrice(averagePrice)
                .priceRange(priceRange)
                .cachedAt(LocalDateTime.now())
                .build();
    }

    public RestaurantInfoResponse toRestaurantInfoResponse(String distance) {
        return RestaurantInfoResponse.builder()
                .id(this.id)
                .name(this.name)
                .address(extractCityAndDistrict(this.address))
                .distance(distance)
                .mainMenus(this.mainMenus)
                .averageRating(this.averageRating)
                .reviewCount(this.reviewCount)
                .isGoodPriceStore(this.isGoodPriceStore)
                .isLocalStore(this.isLocalStore)
                .image(this.image)
                .build();
    }

    private String extractCityAndDistrict(String fullAddress) {
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
