package com.toktot.web.controller;

import com.toktot.interceptor.scheduler.SeogwipoGoodPriceScheduler;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/seogwipo/good-price/scheduler")
@RequiredArgsConstructor
public class SeogwipoSchedulerController {

    private final SeogwipoGoodPriceScheduler seogwipoGoodPriceScheduler;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> runSeogwipoGoodPriceScheduler() {
        log.info("스케줄러 수동 실행 요청");

        seogwipoGoodPriceScheduler.syncSeogwipoGoodPriceStores();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
