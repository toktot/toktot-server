package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.request.RestaurantSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.web.dto.restaurant.response.RestaurantInfoResponse;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantSearchService {

    private final RestaurantRepository restaurantRepository;
    private final KakaoMapService kakaoMapService;
    private final RestaurantLocationMatcher locationMatcher;

    public RestaurantSearchResponse searchFromKakaoWithPagination(RestaurantSearchRequest request) {
        validateSearchRequest(request);

        try {
            RestaurantSearchRequest currentRequest = request;
            KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(currentRequest);

            while (kakaoResponse.getPlaceInfos().size() < KakaoApiConstants.DEFAULT_SIZE && kakaoResponse.hasMorePages()) {
                currentRequest = currentRequest.nextPage();
                KakaoPlaceSearchResponse currentPageResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(currentRequest);
                kakaoResponse = currentPageResponse.mergeWithPreviousResults(kakaoResponse);
            }

            RestaurantSearchResponse basicResponse = RestaurantSearchResponse.from(kakaoResponse, currentRequest.page());
            return combineKakaoAndDbData(basicResponse);

        } catch (Exception e) {
            log.error("Kakao search failed for request: {}", request, e);
            throw new ToktotException(ErrorCode.KAKAO_LOCAL_SERVICE_ERROR, e.getMessage());
        }
    }

    private RestaurantSearchResponse combineKakaoAndDbData(RestaurantSearchResponse kakaoResponse) {
        List<RestaurantInfoResponse> restaurants = kakaoResponse.places();
        List<RestaurantInfoResponse> combinedRestaurants = new ArrayList<>();

        for (RestaurantInfoResponse restaurant : restaurants) {
            // 1. 카카오 ID로 정확 매칭 시도
            Optional<Restaurant> exactMatch = restaurantRepository.findByExternalKakaoId(restaurant.externalKakaoId());

            if (exactMatch.isPresent()) {
                combinedRestaurants.add(buildEnhancedResponse(restaurant, exactMatch.get()));
                log.debug("카카오 ID 매칭: {} (ID: {})", exactMatch.get().getName(), exactMatch.get().getId());
                continue;
            }

            // 2. 위치+매장명 기반 매칭 시도
            Optional<Restaurant> locationMatch = locationMatcher.findExistingRestaurant(
                    restaurant.name(),
                    restaurant.latitude(),
                    restaurant.longitude()
            );

            if (locationMatch.isPresent()) {
                Restaurant matched = locationMatch.get();
                // 카카오 ID가 없으면 업데이트
                if (matched.getExternalKakaoId() == null) {
                    updateKakaoId(matched, restaurant.externalKakaoId());
                }
                combinedRestaurants.add(buildEnhancedResponse(restaurant, matched));
                log.debug("위치 매칭: {} → {} (ID: {})", restaurant.name(), matched.getName(), matched.getId());
                continue;
            }

            // 3. DB 매칭 실패 시 카카오 데이터만 사용
            combinedRestaurants.add(restaurant);
            log.debug("카카오 전용: {}", restaurant.name());
        }

        return new RestaurantSearchResponse(
                combinedRestaurants,
                kakaoResponse.currentPage(),
                kakaoResponse.is_end()
        );
    }

    private RestaurantInfoResponse buildEnhancedResponse(RestaurantInfoResponse kakaoData, Restaurant dbEntity) {
        return RestaurantInfoResponse.builder()
                .id(dbEntity.getId())
                .externalKakaoId(kakaoData.externalKakaoId())
                .name(dbEntity.getName())
                .address(kakaoData.address()) // 카카오 주소 사용 (더 정확)
                .distance(kakaoData.distance())
                .mainMenus(dbEntity.getPopularMenus())
                .longitude(kakaoData.longitude()) // 카카오 좌표 사용
                .latitude(kakaoData.latitude())
                .isGoodPriceStore(dbEntity.getIsGoodPriceStore())
                .isLocalStore(dbEntity.getIsLocalStore())
                .image(dbEntity.getImage())
                .build();
    }

    @Transactional
    public void updateKakaoId(Restaurant restaurant, String kakaoId) {
        restaurant.updateKakaoId(kakaoId); // Restaurant 엔티티에 메서드 추가 필요
        restaurantRepository.save(restaurant);
        log.info("카카오 ID 업데이트: {} → {}", restaurant.getName(), kakaoId);
    }

    private void validateSearchRequest(RestaurantSearchRequest request) {
        if (request.query() == null || request.query().trim().isEmpty()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
        }
    }
}
