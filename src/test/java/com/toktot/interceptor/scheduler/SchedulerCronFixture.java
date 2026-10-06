package com.toktot.interceptor.scheduler;

import java.util.LinkedHashMap;
import java.util.Map;

final class SchedulerCronFixture {

    static final String PREFIX = "toktot.scheduler.cron.";

    static final Map<String, String> PRODUCTION_CRON_BY_KEY = new LinkedHashMap<>();
    static final Map<String, String> KEY_BY_METHOD = new LinkedHashMap<>();

    static {
        register("RestaurantScheduler.scheduledBaseData", "tour-api-base-data", "0 0 2 * * *");
        register("RestaurantScheduler.scheduledDetailIntroSync", "tour-api-detail-intro", "0 20 2 * * *");
        register("RestaurantScheduler.syncRestaurantDetailCommon", "tour-api-detail-common", "0 40 2 * * *");
        register("RestaurantScheduler.syncKakaoAndTourApiData", "kakao-match", "0 0 3 * * *");
        register("SeogwipoGoodPriceScheduler.syncSeogwipoGoodPriceStores", "seogwipo-good-price", "0 20 3 * * *");
        register("JejuGoodPriceScheduler.syncJejuGoodPriceStores", "jeju-good-price", "0 30 3 * * *");
        register("GoodPriceBatchScheduler.updateGoodPriceCache", "good-price-cache", "0 40 3 * * *");
        register("ReviewScheduler.cachePopularReviews", "popular-review", "0 0 * * * *");
    }

    private SchedulerCronFixture() {
    }

    private static void register(String method, String key, String cron) {
        KEY_BY_METHOD.put(method, key);
        PRODUCTION_CRON_BY_KEY.put(key, cron);
    }
}
