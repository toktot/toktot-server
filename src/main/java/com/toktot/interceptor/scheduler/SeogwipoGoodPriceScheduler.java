package com.toktot.interceptor.scheduler;

import com.toktot.external.seogwipo.config.SeogwipoGoodPriceProperties;
import com.toktot.external.seogwipo.service.SeogwipoGoodPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seogwipo.good-price.enabled-batch", havingValue = "true", matchIfMissing = true)
public class SeogwipoGoodPriceScheduler {

    private final SeogwipoGoodPriceProperties properties;
    private final SeogwipoGoodPriceService seogwipoGoodPriceService;

    @Scheduled(cron = "0 20 3 * * *", zone = "Asia/Seoul")
    public void syncSeogwipoGoodPriceStores() {
        if (!properties.isEnabledBatch()) {
            log.info("서귀포 착한가격업소 배치가 비활성화되어 있습니다.");
            return;
        }

        log.info("서귀포 착한가격업소 배치 스케줄러 시작");

        try {
            int result = seogwipoGoodPriceService.syncAllSeogwipoGoodPriceStores();
            log.info("서귀포 착한가격업소 배치 스케줄러 완료: {} 건 처리", result);
        } catch (Exception e) {
            log.error("서귀포 착한가격업소 배치 스케줄러 실행 중 오류 발생", e);
        }
    }
}
