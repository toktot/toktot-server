package com.toktot.domain.localfood.controller;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.localfood.dto.LocalFoodStatsResponse;
import com.toktot.domain.localfood.dto.PriceRangeRequest;
import com.toktot.domain.localfood.service.LocalFoodStatisticsService;
import com.toktot.domain.restaurant.dto.response.PriceRangeRestaurantResponse;
import com.toktot.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/local-foods")
@RequiredArgsConstructor
public class LocalFoodStatsController {

    private final LocalFoodStatisticsService statisticsService;

    @GetMapping("/{localFoodType}/stats")
    public ResponseEntity<ApiResponse<LocalFoodStatsResponse>> getLocalFoodStats(
            @PathVariable LocalFoodType localFoodType
    ) {
        log.info("향토음식 통계 조회 요청 - 타입: {}", localFoodType.getDisplayName());

        LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(localFoodType);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/price-range/restaurants")
    public ResponseEntity<ApiResponse<Page<PriceRangeRestaurantResponse>>> getRestaurantsByPriceRange(
            @Valid @RequestBody PriceRangeRequest request,
            @PageableDefault(size = 20, sort = "distance", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.atInfo()
                .setMessage("가격대별 가게 조회 요청")
                .addKeyValue("localFoodType", request.localFoodType().getDisplayName())
                .addKeyValue("clickedPrice", request.clickedPrice())
                .addKeyValue("priceRange", String.format("%d~%d원", request.getMinPrice(), request.getMaxPrice()))
                .addKeyValue("radius", request.getRadius())
                .log();

        Page<PriceRangeRestaurantResponse> restaurants =
                statisticsService.getRestaurantsByPriceRange(request, pageable);

        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }
}
