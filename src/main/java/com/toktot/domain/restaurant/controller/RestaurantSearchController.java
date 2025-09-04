package com.toktot.domain.restaurant.controller;

import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.domain.restaurant.service.RestaurantSearchService;
import com.toktot.domain.restaurant.dto.request.RestaurantSearchRequest;
import com.toktot.domain.review.service.ReviewFilterService;
import com.toktot.domain.search.type.SortType;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.restaurant.dto.response.RestaurantSearchResponse;
import com.toktot.web.dto.request.SearchCriteria;
import com.toktot.web.dto.request.SearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantSearchController {

    private final RestaurantSearchService restaurantSearchService;
    private final ReviewFilterService reviewFilterService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<RestaurantSearchResponse>> searchRestaurantsFromKakao(
            @Valid @RequestBody RestaurantSearchRequest request) {
        log.info("Kakao restaurant search request: {}", request);

        RestaurantSearchResponse result = restaurantSearchService.searchFromKakaoWithPagination(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/search/filter")
    public ResponseEntity<ApiResponse<Page<RestaurantInfoResponse>>> searchRestaurantsWithFilters(
            @Valid @RequestBody SearchRequest request,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        log.atInfo()
                .setMessage("식당 필터 검색 요청")
                .addKeyValue("query", request.query())
                .addKeyValue("userId", user != null ? user.getId() : null)
                .log();

        SearchCriteria criteria = reviewFilterService.validateAndConvert(request);

        Pageable adjustedPageable = createPageableWithSort(pageable, criteria.sort());

        Page<RestaurantInfoResponse> response = restaurantSearchService.searchRestaurantsWithFilters(
                criteria,
                user != null ? user.getId() : null,
                adjustedPageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Pageable createPageableWithSort(Pageable pageable, SortType sortType) {
        if (sortType == null) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
