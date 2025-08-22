package com.toktot.web.dto.restaurant.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import lombok.Builder;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Builder
public record RestaurantInfoResponse(
        Long id,
        @JsonProperty("external_kakao_id")
        String externalKakaoId,

        String name,
        String address,
        String distance,

        @JsonProperty("main_menus")
        String mainMenus,

        @JsonProperty("average_rating")
        BigDecimal averageRating,

        @JsonProperty("review_count")
        Long reviewCount,

        BigDecimal longitude,

        BigDecimal latitude,

        @JsonProperty("is_good_price_store")
        Boolean isGoodPriceStore,

        @JsonProperty("is_local_store")
        Boolean isLocalStore,
        String image,
        Integer point,
        Integer percent
) {

    public static RestaurantInfoResponse from(KakaoPlaceInfo kakaoPlaceInfo) {
        return RestaurantInfoResponse.builder()
                .externalKakaoId(kakaoPlaceInfo.getId())
                .name(kakaoPlaceInfo.getPlaceName())
                .address(extractCityAndDistrict(kakaoPlaceInfo.getAddressName()))
                .distance(getDistance(kakaoPlaceInfo.getDistance()))
                .longitude(kakaoPlaceInfo.getLongitude())
                .latitude(kakaoPlaceInfo.getLatitude())
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
