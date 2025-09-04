package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.domain.restaurant.dto.request.RestaurantSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(request.query(), currentPage, request.location(), request.sort());

        List<RestaurantInfoResponse> restaurantInfoResponses = new ArrayList<>(
                processAndSaveKakaoResults(kakaoResponse.placeInfos())
        );

        boolean isEnd = kakaoResponse.isEnd();

        while (restaurantInfoResponses.size() < KakaoApiConstants.DEFAULT_SIZE && !isEnd) {
            currentPage++;
            KakaoPlaceSearchResponse additionalResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(request.query(), currentPage, request.location(), request.sort());

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
        Optional<Restaurant> optionalRestaurant = restaurantRepository.findByExternalKakaoIdAndIsActive(kakaoPlaceInfo.getId(), true);

        Restaurant restaurant = optionalRestaurant.orElseGet(() -> {
            Restaurant newRestaurant = kakaoPlaceInfo.toEntity();
            return restaurantRepository.save(newRestaurant);
        });

        return RestaurantInfoResponse.from(restaurant, kakaoPlaceInfo);
    }

    public RestaurantDetailResponse searchRestaurantDetail(Long id) {
        return restaurantRepository.findById(id)
                .map(RestaurantDetailResponse::from)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND));
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
