package com.toktot.domain.localfood.controller;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.localfood.dto.LocalFoodStatsResponse;
import com.toktot.domain.localfood.service.LocalFoodStatisticsService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/local-foods")
@RequiredArgsConstructor
public class LocalFoodStatisticsController {

    private final LocalFoodStatisticsService statisticsService;

    @GetMapping("/{type}/statistics")
    public ResponseEntity<ApiResponse<LocalFoodStatsResponse>> getStatistics(
            @PathVariable LocalFoodType type) {

        log.info("향토음식 통계 조회 요청 - 타입: {}", type.getDisplayName());

        LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(type);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
