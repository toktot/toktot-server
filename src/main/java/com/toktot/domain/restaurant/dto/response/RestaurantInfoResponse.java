package com.toktot.domain.restaurant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import lombok.Builder;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Builder
public record RestaurantInfoResponse(
        Long id,
        String name,
        String address,
        String distance,
        @JsonProperty("main_menus") String mainMenus,
        @JsonProperty("average_rating") BigDecimal averageRating,
        @JsonProperty("review_count") Long reviewCount,
        @JsonProperty("is_good_price_store") Boolean isGoodPriceStore,
        @JsonProperty("is_local_store") Boolean isLocalStore,
        String image,
        Integer point,
        String percent
) {

    public static RestaurantInfoResponse from(Restaurant entity, KakaoPlaceInfo kakaoPlaceInfo) {
        return RestaurantInfoResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(extractCityAndDistrict(kakaoPlaceInfo.getAddressName()))
                .distance(getDistance(kakaoPlaceInfo.getDistance()))
                .mainMenus(entity.getPopularMenus())
                .averageRating(null)
                .reviewCount(0L)
                .isGoodPriceStore(entity.getIsGoodPriceStore())
                .isLocalStore(entity.getIsLocalStore())
                .image(entity.getImage())
                .point(null)
                .percent(null)
                .build();
    }

    public static RestaurantInfoResponse from(KakaoPlaceInfo kakaoPlaceInfo) {
        return RestaurantInfoResponse.builder()
                .id(null)
                .name(kakaoPlaceInfo.getPlaceName())
                .address(extractCityAndDistrict(kakaoPlaceInfo.getAddressName()))
                .distance(getDistance(kakaoPlaceInfo.getDistance()))
                .mainMenus(null)
                .averageRating(null)
                .reviewCount(0L)
                .isGoodPriceStore(false)
                .isLocalStore(false)
                .image(null)
                .point(null)
                .percent(null)
                .build();
    }

    public static RestaurantInfoResponse withStatsComplete(Restaurant entity, KakaoPlaceInfo kakaoPlaceInfo,
                                                           BigDecimal averageRating, Long reviewCount, String distance,
                                                           Integer valueForMoneyPoint, String pricePercentile) {
        return RestaurantInfoResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(extractCityAndDistrict(kakaoPlaceInfo != null ?
                        kakaoPlaceInfo.getAddressName() : entity.getAddress()))
                .distance(distance)
                .mainMenus(entity.getPopularMenus())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .isGoodPriceStore(entity.getIsGoodPriceStore())
                .isLocalStore(entity.getIsLocalStore())
                .image(entity.getImage())
                .point(valueForMoneyPoint)
                .percent(pricePercentile)
                .build();
    }

    private static String getDistance(String distance) {
        if (distance == null || distance.isEmpty() || distance.isBlank()) {
            return null;
        }
        return distance;
    }

    private static String extractCityAndDistrict(String fullAddress) {
        if (!StringUtils.hasText(fullAddress)) {
            return null;
        }

        String[] parts = fullAddress.replace("제주특별자치도", "")
                .trim()
                .split("\\s+");

        if (parts.length < 2) {
            return null;
        }

        return parts[0] + " " + parts[1];
    }
}
