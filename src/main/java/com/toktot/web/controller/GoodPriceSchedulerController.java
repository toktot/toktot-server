package com.toktot.web.controller;


import com.toktot.domain.restaurant.service.GoodPriceCacheService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/good-price/scheduler")
@RequiredArgsConstructor
public class GoodPriceSchedulerController {

    private final GoodPriceCacheService goodPriceCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshCache() {
        log.info("착한가격업소 캐시 수동 갱신 요청");

        try {
            goodPriceCacheService.cacheGoodPriceRestaurantsByPriceRange();

            String message = "착한가격업소 캐시가 성공적으로 갱신되었습니다.";
            log.info(message);

            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (Exception e) {
            String errorMessage = "착한가격업소 캐시 갱신 중 오류가 발생했습니다: " + e.getMessage();
            log.error(errorMessage, e);

            return ResponseEntity.ok(ApiResponse.success(errorMessage));
        }
    }

}
