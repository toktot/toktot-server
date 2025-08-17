package com.toktot.external.kakao;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.kakao.dto.response.KakaoPlace;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Component
public class KakaoPlaceMapper {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{2,3}-\\d{3,4}-\\d{4}$");
    private static final String JEJU_REGION_PREFIX = "제주";

    public Restaurant toRestaurant(KakaoPlace kakaoPlace) {
        if (kakaoPlace == null || !kakaoPlace.hasValidCoordinates()) {
            return null;
        }

        return Restaurant.builder()
                .externalKakaoId(kakaoPlace.getId())
                .name(normalizeName(kakaoPlace.getPlaceName()))
                .category(mapMainCategory(kakaoPlace.getMainCategory()))
                .address(normalizeAddress(kakaoPlace.getAddressName()))
                .roadAddress(normalizeAddress(kakaoPlace.getRoadAddressName()))
                .latitude(kakaoPlace.getLatitude())
                .longitude(kakaoPlace.getLongitude())
                .phone(normalizePhone(kakaoPlace.getPhone()))
                .dataSource(DataSource.KAKAO)
                .isActive(true)
                .searchCount(0)
                .lastSyncedAt(LocalDateTime.now())
                .build();
    }

    public Restaurant updateRestaurantFromKakao(Restaurant existing, KakaoPlace kakaoPlace) {
        if (existing == null || kakaoPlace == null) {
            return existing;
        }

        return Restaurant.builder()
                .id(existing.getId())
                .externalTourApiId(existing.getExternalTourApiId())
                .externalKakaoId(kakaoPlace.getId())
                .name(normalizeName(kakaoPlace.getPlaceName()))
                .category(mapMainCategory(kakaoPlace.getMainCategory()))
                .address(normalizeAddress(kakaoPlace.getAddressName()))
                .roadAddress(normalizeAddress(kakaoPlace.getRoadAddressName()))
                .latitude(kakaoPlace.getLatitude())
                .longitude(kakaoPlace.getLongitude())
                .phone(normalizePhone(kakaoPlace.getPhone()))
                .isGoodPriceStore(existing.getIsGoodPriceStore())
                .dataSource(existing.getDataSource() == DataSource.USER_CREATED ?
                        existing.getDataSource() : DataSource.KAKAO)
                .isActive(existing.getIsActive())
                .searchCount(existing.getSearchCount())
                .lastSyncedAt(LocalDateTime.now())
                .createdAt(existing.getCreatedAt())
                .build();
    }

    public boolean isJejuRestaurant(KakaoPlace kakaoPlace) {
        if (kakaoPlace.getAddressName() == null) {
            return false;
        }
        return kakaoPlace.getAddressName().startsWith(JEJU_REGION_PREFIX);
    }

    public boolean isValidRestaurant(KakaoPlace kakaoPlace) {
        return kakaoPlace != null
                && kakaoPlace.hasValidCoordinates()
                && kakaoPlace.getPlaceName() != null
                && !kakaoPlace.getPlaceName().trim().isEmpty()
                && kakaoPlace.isFoodCategory();
    }

    private String normalizeName(String name) {
        if (name == null) return null;
        return name.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\[\\](){}]", "");
    }

    private String mapMainCategory(String categoryName) {
        if (categoryName == null) return "음식점";

        switch (categoryName) {
            case "음식점":
                return "음식점";
            case "카페":
                return "카페";
            default:
                return categoryName.contains("음식") || categoryName.contains("식당") ? "음식점" : "카페";
        }
    }

    private String normalizeAddress(String address) {
        if (address == null) return null;
        return address.trim()
                .replaceAll("제주특별자치도", "제주도")
                .replaceAll("\\s+", " ");
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String normalized = phone.trim()
                .replaceAll("[^0-9-]", "")
                .replaceAll("^\\+82-?", "0");

        if (PHONE_PATTERN.matcher(normalized).matches()) {
            return normalized;
        }
        return null;
    }
}
