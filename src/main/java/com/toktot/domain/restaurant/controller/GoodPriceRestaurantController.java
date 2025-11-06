package com.toktot.web.controller;

import com.toktot.common.util.PriceParserUtil;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.domain.restaurant.service.GoodPriceRestaurantService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants/good-price")
@RequiredArgsConstructor
public class GoodPriceRestaurantController {

    private final GoodPriceRestaurantService goodPriceRestaurantService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RestaurantInfoResponse>>> getGoodPriceRestaurants(
            @RequestParam(value = "priceRange", required = false) Integer priceRange,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {

        log.info("착한가격업소 조회 요청 - 가격대: {}, 위치: ({}, {}), 사용자: {}",
                PriceParserUtil.getPriceRangeName(priceRange),
                latitude, longitude,
                user != null ? user.getId() : "익명");

        Page<RestaurantInfoResponse> restaurants = goodPriceRestaurantService
                .getGoodPriceRestaurants(priceRange, longitude, latitude, pageable);

        log.info("착한가격업소 조회 완료 - 가격대: {}, 결과: {}개, 전체: {}개",
                PriceParserUtil.getPriceRangeName(priceRange),
                restaurants.getContent().size(),
                restaurants.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }
}
