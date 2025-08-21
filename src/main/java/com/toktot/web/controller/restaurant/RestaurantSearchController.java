package com.toktot.web.controller.restaurant;

import com.toktot.domain.restaurant.service.RestaurantSearchService;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.request.KakaoPlaceSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponse;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponseTemp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantSearchController {

    private final RestaurantSearchService restaurantSearchService;
    private final KakaoMapService kakaoMapService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RestaurantSearchResponseTemp>>> searchRestaurants() {
        List<RestaurantSearchResponseTemp> response = restaurantSearchService.getRestaurantResponse();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/kakao/search")
    public ResponseEntity<ApiResponse<RestaurantSearchResponse>> searchRestaurantsKakao(@RequestBody KakaoPlaceSearchRequest request) {
        log.info("request={}", request.toString());
        KakaoPlaceSearchResponse response = kakaoMapService.searchJejuAllFoodAndCafePlace(request);
        while(response.getPlaceInfos().size() < KakaoApiConstants.DEFAULT_SIZE && response.hasMorePages()) {
            request = request.plusPage();
            KakaoPlaceSearchResponse newResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(request);
            response = newResponse.addPlaceInfo(response);
        }

        RestaurantSearchResponse result = RestaurantSearchResponse.from(response, request.page());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
