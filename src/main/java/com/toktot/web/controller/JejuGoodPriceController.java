package com.toktot.web.controller;

import com.toktot.external.jeju.service.JejuGoodPriceService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/jeju/good-price/scheduler")
@RequiredArgsConstructor
public class JejuGoodPriceController {

    private final JejuGoodPriceService jejuGoodPriceService;

    @PostMapping
    public ResponseEntity<String> syncJejuGoodPriceStores() {
        log.info("제주시 착한가격업소 수동 동기화 요청");

        int processedCount = jejuGoodPriceService.syncAllJejuGoodPriceStores();

        log.info("제주시 착한가격업소 수동 동기화 완료: {} 건 처리", processedCount);

        return ResponseEntity.ok(String.format("제주시 착한가격업소 동기화가 완료되었습니다. 처리된 건수: %d", processedCount));

    }
}
