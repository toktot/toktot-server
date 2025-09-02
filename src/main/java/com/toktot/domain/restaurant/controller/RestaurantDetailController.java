package com.toktot.domain.restaurant.controller;

import com.toktot.domain.restaurant.service.RestaurantSearchService;
import com.toktot.domain.review.service.RestaurantReviewService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.toktot.domain.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants/{restaurantId}")
@RequiredArgsConstructor
public class RestaurantDetailController {

    private final RestaurantReviewService restaurantReviewService;
    private final RestaurantSearchService restaurantSearchService;

    @GetMapping
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> getRestaurantDetail(
            @PathVariable @Positive Long restaurantId) {
        log.info("get: restaurant detail - restaurants.id = {}", restaurantId);

        RestaurantDetailResponse response = restaurantSearchService.searchRestaurantDetail(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getRestaurantReviews(
            @PathVariable @Positive Long restaurantId,
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        log.atInfo()
                .setMessage("Restaurant reviews for " + restaurantId + " are available")
                .addKeyValue("restaurantId", restaurantId)
                .log();
        Page<ReviewResponse> response = restaurantReviewService.getRestaurantReviews(restaurantId, pageable, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
