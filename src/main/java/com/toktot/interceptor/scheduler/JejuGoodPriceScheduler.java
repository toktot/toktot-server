package com.toktot.interceptor.scheduler;


import com.toktot.external.jeju.config.JejuGoodPriceProperties;
import com.toktot.external.jeju.service.JejuGoodPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jeju.good-price.enabled-batch", havingValue = "true", matchIfMissing = true)
public class JejuGoodPriceScheduler {

    private final JejuGoodPriceProperties properties;
    private final JejuGoodPriceService jejuGoodPriceService;

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    public void syncJejuGoodPriceStores() {
        if (!properties.isEnabledBatch()) {
            log.info("제주시 착한가격업소 배치가 비활성화되어 있습니다.");
            return;
        }

        log.info("제주시 착한가격업소 배치 스케줄러 시작");

        try {
            int result = jejuGoodPriceService.syncAllJejuGoodPriceStores();
            log.info("제주시 착한가격업소 배치 스케줄러 완료: {} 건 처리", result);
        } catch (Exception e) {
            log.error("제주시 착한가격업소 배치 스케줄러 실행 중 오류 발생", e);
        }
    }
}
