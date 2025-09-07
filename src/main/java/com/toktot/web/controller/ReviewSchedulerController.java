package com.toktot.web.controller;

import com.toktot.interceptor.scheduler.ReviewScheduler;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/review/scheduler")
@RequiredArgsConstructor
public class ReviewSchedulerController {

    private final ReviewScheduler reviewScheduler;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> runPopularReviewsScheduler() {
        log.info("스케줄러 수동 실행 요청");

        try {
            reviewScheduler.cachePopularReviews();
            return ResponseEntity.ok(ApiResponse.success("스케줄러가 성공적으로 실행되었습니다."));
        } catch (Exception e) {
            log.error("스케줄러 수동 실행 실패", e);
            return ResponseEntity.ok(ApiResponse.success("스케줄러 실행 실패: " + e.getMessage()));
        }
    }
}
