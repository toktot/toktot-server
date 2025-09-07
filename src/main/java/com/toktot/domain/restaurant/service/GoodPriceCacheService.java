package com.toktot.domain.restaurant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.util.PriceParserUtil;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.dto.cache.GoodPriceRestaurantDto;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodPriceCacheService {

    private final RestaurantRepository restaurantRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "good_price_restaurants:";
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    @Transactional(readOnly = true)
    public void cacheGoodPriceRestaurantsByPriceRange() {
        log.info("착한가격업소 가격대별 캐시 작업 시작");

        try {
            List<Restaurant> goodPriceRestaurants = restaurantRepository
                    .findByIsGoodPriceStoreAndIsActive(true, true);

            log.info("총 {} 개의 착한가격업소 발견", goodPriceRestaurants.size());

            Map<Integer, List<GoodPriceRestaurantDto>> restaurantsByPriceRange = classifyByPriceRange(goodPriceRestaurants);

            for (Map.Entry<Integer, List<GoodPriceRestaurantDto>> entry : restaurantsByPriceRange.entrySet()) {
                Integer priceRange = entry.getKey();
                List<GoodPriceRestaurantDto> restaurants = entry.getValue();

                String redisKey = REDIS_KEY_PREFIX + priceRange;
                String jsonValue = objectMapper.writeValueAsString(restaurants);

                redisTemplate.opsForValue().set(redisKey, jsonValue, CACHE_TTL);

                log.info("가격대 {} 캐시 완료: {} 개 매장",
                        PriceParserUtil.getPriceRangeName(priceRange), restaurants.size());
            }

            log.info("착한가격업소 가격대별 캐시 작업 완료");

        } catch (Exception e) {
            log.error("착한가격업소 캐시 작업 실패", e);
            throw new RuntimeException("착한가격업소 캐시 작업 실패", e);
        }
    }

    public List<GoodPriceRestaurantDto> getGoodPriceRestaurantsByPriceRange(Integer priceRange) {
        try {
            String redisKey = REDIS_KEY_PREFIX + priceRange;
            String jsonValue = redisTemplate.opsForValue().get(redisKey);

            if (jsonValue == null) {
                log.warn("Redis에서 가격대 {} 데이터를 찾을 수 없음",
                        PriceParserUtil.getPriceRangeName(priceRange));
                return Collections.emptyList();
            }

            List<GoodPriceRestaurantDto> restaurants = objectMapper.readValue(
                    jsonValue,
                    new TypeReference<List<GoodPriceRestaurantDto>>() {}
            );

            log.debug("가격대 {} 캐시에서 {} 개 매장 조회",
                    PriceParserUtil.getPriceRangeName(priceRange), restaurants.size());

            return restaurants;

        } catch (Exception e) {
            log.error("Redis에서 가격대별 매장 조회 실패: priceRange={}", priceRange, e);
            return Collections.emptyList();
        }
    }

    private Map<Integer, List<GoodPriceRestaurantDto>> classifyByPriceRange(List<Restaurant> restaurants) {
        Map<Integer, List<GoodPriceRestaurantDto>> result = new HashMap<>();

        Arrays.asList(0, 10_000, 20_000, 30_000, 50_000, 70_000)
                .forEach(priceRange -> result.put(priceRange, new ArrayList<>()));

        for (Restaurant restaurant : restaurants) {
            try {
                Double averagePrice = PriceParserUtil.calculateAveragePrice(restaurant.getPopularMenus());

                if (averagePrice == null) {
                    log.debug("가격 파싱 실패로 매장 제외: {} (메뉴: {})",
                            restaurant.getName(), restaurant.getPopularMenus());
                    continue;
                }

                Integer priceRange = PriceParserUtil.determinePriceRange(averagePrice);

                if (priceRange == null) {
                    log.debug("가격대 결정 실패로 매장 제외: {} (평균가격: {})",
                            restaurant.getName(), averagePrice);
                    continue;
                }

                GoodPriceRestaurantDto dto = GoodPriceRestaurantDto.fromWithoutStats(restaurant, averagePrice, priceRange);
                result.get(priceRange).add(dto);

                log.debug("매장 분류 완료: {} -> {} (평균가격: {})",
                        restaurant.getName(), PriceParserUtil.getPriceRangeName(priceRange), averagePrice);

            } catch (Exception e) {
                log.error("매장 분류 중 오류 발생: {} (메뉴: {})",
                        restaurant.getName(), restaurant.getPopularMenus(), e);
            }
        }

        result.forEach((priceRange, restaurantList) -> {
            log.info("가격대 분류 결과 - {}: {} 개 매장",
                    PriceParserUtil.getPriceRangeName(priceRange), restaurantList.size());
        });

        return result;
    }
}
