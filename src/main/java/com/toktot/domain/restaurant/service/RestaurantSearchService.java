package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.request.RestaurantSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.web.dto.restaurant.response.RestaurantDetailResponse;
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

    @Transactional
    public RestaurantSearchResponse searchFromKakaoWithPagination(RestaurantSearchRequest request) {
        validateSearchRequest(request);

        int currentPage = request.page();
        Boolean isEnd = true;
        List<RestaurantInfoResponse> restaurantInfoResponses = new ArrayList<>();
        String query = request.query();
        try {
            KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(query, currentPage);
            isEnd = kakaoResponse.isEnd();
            for (KakaoPlaceInfo kakaoPlaceInfo : kakaoResponse.placeInfos()) {
                Optional<Restaurant> byExternalKakaoId = restaurantRepository.findByExternalKakaoId(kakaoPlaceInfo.getId());

                if (byExternalKakaoId.isPresent()) {
                    Restaurant restaurant = byExternalKakaoId.get();
                    restaurantInfoResponses.add(RestaurantInfoResponse.from(restaurant, kakaoPlaceInfo));
                } else {
                    Restaurant restaurant = kakaoPlaceInfo.toEntity();
                    restaurantRepository.save(restaurant);
                    restaurantInfoResponses.add(RestaurantInfoResponse.from(restaurant, kakaoPlaceInfo));
                }
            }

            while (restaurantInfoResponses.size() < KakaoApiConstants.DEFAULT_SIZE && !isEnd) {
                currentPage++;
                KakaoPlaceSearchResponse addKakaoResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(query, currentPage);
                isEnd = addKakaoResponse.isEnd();

                for (KakaoPlaceInfo kakaoPlaceInfo : addKakaoResponse.placeInfos()) {
                    Optional<Restaurant> byExternalKakaoId = restaurantRepository.findByExternalKakaoId(kakaoPlaceInfo.getId());

                    if (byExternalKakaoId.isPresent()) {
                        Restaurant restaurant = byExternalKakaoId.get();
                        restaurantInfoResponses.add(RestaurantInfoResponse.from(restaurant, kakaoPlaceInfo));
                    } else {
                        Restaurant restaurant = kakaoPlaceInfo.toEntity();
                        restaurantRepository.save(restaurant);
                        restaurantInfoResponses.add(RestaurantInfoResponse.from(restaurant, kakaoPlaceInfo));
                    }
                }
            }

            return RestaurantSearchResponse.from(restaurantInfoResponses, currentPage, isEnd);
        } catch (Exception e) {
            log.error("Kakao search failed for request: {}", request, e);
            throw new ToktotException(ErrorCode.KAKAO_LOCAL_SERVICE_ERROR, e.getMessage());
        }
    }

    public RestaurantDetailResponse searchRestaurantDetail(Long id) {
        return restaurantRepository.findById(id)
                .map(RestaurantDetailResponse::from)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND));
    }

    private void validateSearchRequest(RestaurantSearchRequest request) {
        if (request.query() == null || request.query().trim().isEmpty()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
        }
    }
}
