package com.toktot.external.kakao.service;

import com.toktot.external.kakao.client.KakaoMapClient;
import com.toktot.external.kakao.KakaoApiProperties;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.request.KakaoPlaceSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoApiResponse;
import com.toktot.external.kakao.dto.response.KakaoPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMapService {

    private final KakaoMapClient kakaoMapClient;
    private final KakaoApiProperties kakaoApiProperties;

    public KakaoApiResponse<KakaoPlace> searchRestaurants(String keyword, BigDecimal longitude,
                                                          BigDecimal latitude, Integer radius) {
        validateSearchParams(keyword, longitude, latitude, radius);

        KakaoPlaceSearchRequest request = KakaoPlaceSearchRequest.keywordWithLocation(
                keyword, longitude, latitude, radius);

        KakaoApiResponse<KakaoPlace> response = kakaoMapClient.searchPlaces(request);

        return filterFoodPlaces(response);
    }

    public KakaoApiResponse<KakaoPlace> searchRestaurantsByKeyword(String keyword) {
        validateKeyword(keyword);

        KakaoPlaceSearchRequest request = KakaoPlaceSearchRequest.keyword(keyword);

        KakaoApiResponse<KakaoPlace> response = kakaoMapClient.searchPlaces(request);

        return filterFoodPlaces(response);
    }

    public KakaoApiResponse<KakaoPlace> searchNearbyRestaurants(BigDecimal longitude,
                                                                BigDecimal latitude, Integer radius) {
        validateLocationParams(longitude, latitude, radius);

        KakaoPlaceSearchRequest request = KakaoPlaceSearchRequest.category(
                KakaoApiConstants.CATEGORY_FOOD, longitude, latitude, radius);

        return kakaoMapClient.searchPlacesByCategory(request);
    }

    public KakaoApiResponse<KakaoPlace> searchNearbyCafes(BigDecimal longitude,
                                                          BigDecimal latitude, Integer radius) {
        validateLocationParams(longitude, latitude, radius);

        KakaoPlaceSearchRequest request = KakaoPlaceSearchRequest.category(
                KakaoApiConstants.CATEGORY_CAFE, longitude, latitude, radius);

        return kakaoMapClient.searchPlacesByCategory(request);
    }

    public List<KakaoPlace> searchAllPages(String keyword, BigDecimal longitude,
                                           BigDecimal latitude, Integer radius, Integer maxPages) {
        List<KakaoPlace> allResults = new ArrayList<>();

        for (int page = 1; page <= maxPages; page++) {
            KakaoPlaceSearchRequest request = KakaoPlaceSearchRequest.keywordWithLocation(
                            keyword, longitude, latitude, radius)
                    .withPaging(page, kakaoApiProperties.getMaxPageSize());

            KakaoApiResponse<KakaoPlace> response = kakaoMapClient.searchPlaces(request);

            if (response.hasResults()) {
                allResults.addAll(response.getDocuments());
            }

            if (!response.hasMorePages()) {
                break;
            }
        }

        return allResults.stream()
                .filter(place -> place.isFoodCategory())
                .collect(Collectors.toList());
    }

    private KakaoApiResponse<KakaoPlace> filterFoodPlaces(KakaoApiResponse<KakaoPlace> response) {
        if (!response.hasResults()) {
            return response;
        }

        List<KakaoPlace> filteredPlaces = response.getDocuments().stream()
                .filter(KakaoPlace::isFoodCategory)
                .collect(Collectors.toList());

        response.setDocuments(filteredPlaces);
        return response;
    }

    private void validateSearchParams(String keyword, BigDecimal longitude,
                                      BigDecimal latitude, Integer radius) {
        validateKeyword(keyword);
        validateLocationParams(longitude, latitude, radius);
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어가 필요합니다");
        }

        if (keyword.trim().length() < 2) {
            throw new IllegalArgumentException("검색어는 최소 2글자 이상이어야 합니다");
        }
    }

    private void validateLocationParams(BigDecimal longitude, BigDecimal latitude, Integer radius) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("위도와 경도가 필요합니다");
        }

        if (radius == null || radius < KakaoApiConstants.MIN_RADIUS || radius > KakaoApiConstants.MAX_RADIUS) {
            throw new IllegalArgumentException(
                    String.format("반경은 %d~%d 미터 사이여야 합니다",
                            KakaoApiConstants.MIN_RADIUS, KakaoApiConstants.MAX_RADIUS));
        }
    }
}
