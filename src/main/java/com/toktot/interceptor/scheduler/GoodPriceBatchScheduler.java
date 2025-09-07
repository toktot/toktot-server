package com.toktot.interceptor.scheduler;

import com.toktot.domain.restaurant.service.GoodPriceCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoodPriceBatchScheduler {

    private final GoodPriceCacheService goodPriceCacheService;

    @Scheduled(cron = "0 40 3 * * *", zone = "Asia/Seoul")
    public void updateGoodPriceCache() {
        log.info("착한가격업소 가격대별 캐시 갱신 배치 시작");

        try {
            goodPriceCacheService.invalidateAllCache();

            goodPriceCacheService.cacheGoodPriceRestaurantsByPriceRange();

            log.info("착한가격업소 가격대별 캐시 갱신 배치 완료");

        } catch (Exception e) {
            log.error("착한가격업소 가격대별 캐시 갱신 배치 실패", e);
        }
    }
}