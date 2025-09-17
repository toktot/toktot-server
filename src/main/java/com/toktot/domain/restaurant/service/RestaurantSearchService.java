package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.block.UserBlockRepository;
import com.toktot.domain.localfood.service.LocalFoodDetectionService;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.repository.RestaurantSearchRepository;
import com.toktot.domain.restaurant.dto.request.RestaurantSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantSearchResponse;
import com.toktot.web.dto.request.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantSearchService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantSearchRepository restaurantSearchRepository;
    private final UserBlockRepository userBlockRepository;
    private final KakaoMapService kakaoMapService;
    private final LocalFoodDetectionService localFoodDetectionService;

    public RestaurantSearchResponse searchFromKakaoWithPagination(RestaurantSearchRequest request) {
        log.info("Kakao 식당 검색 요청 - query: {}, lat: {}, lng: {}",
                request.query(), request.latitude(), request.longitude());

        KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(
                request.query(),
                request.page(),
                request.location(),
                request.sort()
        );

        return RestaurantSearchResponse.from(kakaoResponse, request.page());
    }

    public Page<RestaurantInfoResponse> searchRestaurantsWithFilters(SearchCriteria criteria,
                                                                     Long currentUserId,
                                                                     Pageable pageable) {
        log.info("필터 기반 식당 검색 - query: {}, userId: {}", criteria.query(), currentUserId);

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        return restaurantSearchRepository.searchRestaurantsWithFilters(criteria, currentUserId, blockedUserIds, pageable);
    }

    public Page<RestaurantInfoResponse> searchLocalFoodRestaurantsWithPriceFilter(SearchCriteria criteria,
                                                                                  Long currentUserId,
                                                                                  Pageable pageable) {
        log.info("향토음식 가격 필터링 식당 검색 - localFoodType: {}, minPrice: {}, maxPrice: {}",
                criteria.localFood().type(), criteria.localFood().minPrice(), criteria.localFood().maxPrice());

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        var tooltips = localFoodDetectionService.findTooltipsByTypeAndPrice(
                criteria.localFood().type(),
                criteria.localFood().minPrice(),
                criteria.localFood().maxPrice()
        );

        List<Long> restaurantIds = tooltips.stream()
                .map(tooltip -> tooltip.getReviewImage().getReview().getRestaurant().getId())
                .distinct()
                .collect(Collectors.toList());

        if (restaurantIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return restaurantSearchRepository.searchRestaurantsByIds(restaurantIds, criteria, currentUserId, blockedUserIds, pageable);
    }

    public RestaurantDetailResponse searchRestaurantDetail(Long restaurantId) {
        log.info("식당 상세 조회 - restaurantId: {}", restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND));

        return RestaurantDetailResponse.from(restaurant);
    }

    private List<Long> getBlockedUserIds(Long currentUserId) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        return userBlockRepository.findBlockedUserIdsByBlockerUserId(currentUserId);
    }
}
