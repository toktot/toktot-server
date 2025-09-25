package com.toktot.domain.restaurant.controller;

import com.toktot.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.toktot.domain.restaurant.service.RestaurantMenuService;
import com.toktot.domain.restaurant.service.RestaurantSearchService;
import com.toktot.domain.review.dto.response.search.RestaurantDetailReviewResponse;
import com.toktot.domain.review.dto.response.search.RestaurantReviewStatisticsResponse;
import com.toktot.domain.review.service.ReviewSearchService;
import com.toktot.domain.search.type.SortType;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants/{restaurantId}")
@RequiredArgsConstructor
public class RestaurantDetailController {

    private final RestaurantMenuService restaurantMenuService;
    private final RestaurantSearchService restaurantSearchService;
    private final ReviewSearchService reviewSearchService;

    @GetMapping
    public ResponseEntity<ApiResponse<RestaurantDetailResponse>> getRestaurantDetail(
            @PathVariable @Positive Long restaurantId) {
        log.info("get: restaurant detail - restaurants.id = {}", restaurantId);

        RestaurantDetailResponse response = restaurantSearchService.searchRestaurantDetail(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<RestaurantMenuResponse>>> getRestaurantMenus(
            @PathVariable Long restaurantId) {

        log.info("get: restaurant menus - restaurants.id = {}", restaurantId);
        List<RestaurantMenuResponse> response = restaurantMenuService.getRestaurantMenus(restaurantId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<RestaurantDetailReviewResponse>>> getRestaurantReviews(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) Long reviewId,
            @RequestParam(required = false) SortType sort,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        log.atInfo()
                .setMessage("가게 상세 페이지 리뷰 조회 요청")
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("reviewId", reviewId)
                .addKeyValue("sortType", sort)
                .addKeyValue("userId", user != null ? user.getId() : null)
                .log();

        Page<RestaurantDetailReviewResponse> response = reviewSearchService.getRestaurantReviews(
                restaurantId,
                reviewId,
                sort,
                user != null ? user.getId() : null,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/review-statistics")
    public ResponseEntity<ApiResponse<RestaurantReviewStatisticsResponse>> getRestaurantReviewStatistics(
            @PathVariable Long restaurantId) {

        log.atInfo()
                .setMessage("가게 리뷰 통계 조회 요청")
                .addKeyValue("restaurantId", restaurantId)
                .log();

        RestaurantReviewStatisticsResponse response = reviewSearchService.getRestaurantReviewStatistics(restaurantId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
