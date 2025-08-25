package com.toktot.web.controller.restaurant;

import com.toktot.common.exception.ErrorCode;
import com.toktot.external.tourapi.TourApiService;
import com.toktot.external.tourapi.TourApiSyncService;
import com.toktot.external.tourapi.dto.BatchResult;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.service.TourApiDetailIntroService;
import com.toktot.external.tourapi.service.TourApiImageService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/restaurant/tour-api")
@RequiredArgsConstructor
public class TourApiSyncController {

    private final TourApiService tourApiService;
    private final TourApiSyncService tourApiSyncService;
    private final TourApiDetailIntroService tourApiDetailIntroService;
    private final TourApiImageService tourApiImageService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<BatchResult>> syncAllRestaurants() {
        log.info("TourAPI 전체 데이터 동기화 요청");

        try {
            BatchResult result = tourApiService.collectAllJejuRestaurants();

            if (result.isCompleted()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
            }

        } catch (Exception e) {
            log.error("데이터 동기화 중 예외 발생", e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
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

    @PostMapping("/images")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncImages() {
        log.info("TourAPI 이미지 동기화 요청");

        try {
            int successCount = tourApiImageService.syncAllRestaurantsImages();

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("syncedAt", LocalDateTime.now());
            result.put("dataType", "DETAIL_IMAGE");

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("이미지 동기화 중 예외 발생", e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        log.info("TourAPI 현재 상태 조회");

        try {
            long totalCount = tourApiSyncService.countTourApiRestaurants();

            Map<String, Object> status = new HashMap<>();
            status.put("totalRestaurants", totalCount);
            status.put("dataSource", "TOUR_API");
            status.put("lastChecked", LocalDateTime.now());
            status.put("isServiceAvailable", true);

            return ResponseEntity.ok(ApiResponse.success(status));

        } catch (Exception e) {
            log.error("상태 조회 중 예외 발생", e);
            return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testApiConnection() {
        log.info("TourAPI 연결 테스트 요청");

        try {
            LocalDateTime startTime = LocalDateTime.now();
            TourApiResponse<TourApiItemsWrapper> response = tourApiService.getRestaurantsByPage(1);
            LocalDateTime endTime = LocalDateTime.now();

            boolean isSuccess = response != null &&
                    response.response() != null &&
                    response.response().header() != null &&
                    "0000".equals(response.response().header().resultCode());

            Map<String, Object> testResult = new HashMap<>();
            testResult.put("isConnected", isSuccess);
            testResult.put("responseTime", Duration.between(startTime, endTime).toMillis() + "ms");
            testResult.put("testedAt", startTime);

            if (isSuccess && response.response().body() != null) {
                testResult.put("totalCount", response.response().body().totalCount());
                testResult.put("sampleData", response.response().body().items());
            } else if (response != null && response.response() != null && response.response().header() != null) {
                testResult.put("errorCode", response.response().header().resultCode());
                testResult.put("errorMessage", response.response().header().resultMsg());
            }

            if (isSuccess) {
                return ResponseEntity.ok(ApiResponse.success(testResult));
            } else {
                return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
            }

        } catch (Exception e) {
            log.error("API 연결 테스트 중 예외 발생", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("isConnected", false);
            errorResult.put("errorMessage", e.getMessage());
            errorResult.put("testedAt", LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR));
        }
    }

}
