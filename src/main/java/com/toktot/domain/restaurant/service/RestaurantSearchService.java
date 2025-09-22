package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.block.UserBlockRepository;
import com.toktot.domain.localfood.service.LocalFoodDetectionService;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.repository.RestaurantSearchRepository;
import com.toktot.domain.restaurant.dto.request.RestaurantSearchRequest;
import com.toktot.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantSearchResponse;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.web.dto.request.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    @Transactional
    public RestaurantSearchResponse searchFromKakaoWithPagination(RestaurantSearchRequest request) {
        validateSearchRequest(request);

        int currentPage = request.page();

        KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(
                request.query(),
                currentPage,
                request.location(),
                request.sort()
        );

        List<RestaurantInfoResponse> restaurantInfoResponses = new ArrayList<>(
                processAndSaveKakaoResults(kakaoResponse.placeInfos())
        );

        boolean isEnd = kakaoResponse.isEnd();

        while (restaurantInfoResponses.size() < KakaoApiConstants.DEFAULT_SIZE && !isEnd) {
            currentPage++;
            KakaoPlaceSearchResponse additionalResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(
                    request.query(),
                    currentPage,
                    request.location(),
                    request.sort()
            );

            restaurantInfoResponses.addAll(processAndSaveKakaoResults(additionalResponse.placeInfos()));
            isEnd = additionalResponse.isEnd();
        }

        return RestaurantSearchResponse.from(restaurantInfoResponses, currentPage, isEnd);
    }

    private List<RestaurantInfoResponse> processAndSaveKakaoResults(List<KakaoPlaceInfo> placeInfos) {
        return placeInfos.stream()
                .map(this::findOrSaveRestaurant)
                .collect(Collectors.toList());
    }

    private RestaurantInfoResponse findOrSaveRestaurant(KakaoPlaceInfo kakaoPlaceInfo) {
        Optional<Restaurant> optionalRestaurant = restaurantRepository.findByExternalKakaoIdAndIsActive(
                kakaoPlaceInfo.getId(), true);

        Restaurant restaurant = optionalRestaurant.orElseGet(() -> {
            Restaurant newRestaurant = kakaoPlaceInfo.toEntity();
            return restaurantRepository.save(newRestaurant);
        });

        return RestaurantInfoResponse.from(restaurant, kakaoPlaceInfo);
    }

    public Page<RestaurantInfoResponse> searchRestaurantsWithFilters(SearchCriteria criteria,
                                                                     Long currentUserId,
                                                                     Pageable pageable) {
        log.info("필터 기반 식당 검색 - query: {}, userId: {}", criteria.query(), currentUserId);

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        return restaurantSearchRepository.searchRestaurantsWithFilters(
                criteria, currentUserId, blockedUserIds, pageable);
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

        return restaurantSearchRepository.searchRestaurantsByIds(
                restaurantIds, criteria, currentUserId, blockedUserIds, pageable);
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

    private void validateSearchRequest(RestaurantSearchRequest request) {
        if (!request.hasQuery()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
        }

        if (!request.hasPage()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "페이지 정보가 누락되었습니다");
        }

        if (request.location() != null && !request.location().isValid()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "위치 정보가 올바르지 않습니다.");
        }
    }
}