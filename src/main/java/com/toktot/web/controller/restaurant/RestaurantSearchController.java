package com.toktot.web.controller.restaurant;

import com.toktot.domain.restaurant.service.RestaurantSearchService;
import com.toktot.external.kakao.dto.request.RestaurantSearchRequest;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantSearchController {

    private final RestaurantSearchService restaurantSearchService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<RestaurantSearchResponse>> searchRestaurantsFromKakao(
            @Valid @RequestBody RestaurantSearchRequest request) {
        log.info("Kakao restaurant search request: {}", request);

        RestaurantSearchResponse result = restaurantSearchService.searchFromKakaoWithPagination(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
