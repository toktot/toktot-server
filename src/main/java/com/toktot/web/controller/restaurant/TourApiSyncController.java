package com.toktot.web.controller.restaurant;

import com.toktot.common.exception.ErrorCode;
import com.toktot.domain.restaurant.service.RestaurantMatchService;
import com.toktot.external.tourapi.service.TourApiDetailCommonService;
import com.toktot.external.tourapi.service.TourApiService;
import com.toktot.external.tourapi.service.TourApiDetailIntroService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/restaurant/tour-api")
@RequiredArgsConstructor
public class TourApiSyncController {

    private final TourApiService tourApiService;
    private final TourApiDetailCommonService tourApiDetailCommonService;
    private final TourApiDetailIntroService tourApiDetailIntroService;
    private final RestaurantMatchService restaurantMatchService;

    @PostMapping("/sync")
    public ResponseEntity<Void> syncAllRestaurants() {
        log.info("TourAPI 전체 데이터 동기화 요청");

        try {
            tourApiService.findRestaurantsInJeju();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("데이터 동기화 중 예외 발생", e);
            return null;
        }
    }

    @PostMapping("/intro")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntro() {
        log.info("TourAPI 상세정보 동기화 요청");

        try {
            int successCount = tourApiDetailIntroService.syncAllRestaurantsDetailIntro();

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("syncedAt", LocalDateTime.now());
            result.put("dataType", "DETAIL_INTRO");

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("상세정보 동기화 중 예외 발생", e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
        }
    }

    @PostMapping("/kakao-id")
    public void addExternalKakaoIdInTourApiRestaurant() {
        log.info("TourAPI 카카오 ID 매칭 배치 스케줄러 시작");

        try {
            restaurantMatchService.addExternalKakaoIdInTourApiRestaurant();

            log.info("TourAPI 카카오 ID 매칭 배치 스케줄러 완료");

        } catch (Exception e) {
            log.error("TourAPI 카카오 ID 매칭 배치 스케줄러 예외 발생 - {}", e.getMessage(), e);
        }
    }

    @PostMapping("/detail-common")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncDetailCommon() {
        try {
            int successCount = tourApiDetailCommonService.syncAllRestaurantsDetailCommon();

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("syncedAt", LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("DetailCommon 동기화 실패", e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
        }
    }

}
