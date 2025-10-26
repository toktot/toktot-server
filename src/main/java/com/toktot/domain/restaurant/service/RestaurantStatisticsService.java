package com.toktot.domain.restaurant.service;

import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.repository.RestaurantMenuRepository;
import com.toktot.domain.review.Tooltip;
import com.toktot.domain.review.repository.TooltipRepository;
import com.toktot.domain.review.type.TooltipType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantStatisticsService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMenuRepository restaurantMenuRepository;
    private final TooltipRepository tooltipRepository;

    public BigDecimal calculateAverageRating(Long restaurantId) {
        List<Tooltip> tooltips = tooltipRepository.findByReviewImageReviewRestaurantIdAndTooltipType(
                restaurantId, TooltipType.FOOD);

        if (tooltips.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double average = tooltips.stream()
                .mapToDouble(tooltip -> tooltip.getRating().doubleValue())
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(average);
    }

    public Long calculateReviewCount(Long restaurantId) {
        return restaurantRepository.countReviewsByRestaurantId(restaurantId);
    }

    public Integer calculateValueForMoneyPoint(Long restaurantId) {
        List<Integer> valueForMoneyScores = restaurantRepository
                .findValueForMoneyScoresByRestaurantId(restaurantId);

        if (valueForMoneyScores.isEmpty()) {
            return null;
        }

        double average = valueForMoneyScores.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return (int) Math.round(average);
    }

    public String calculatePricePercentile(Long restaurantId) {
        Integer mainMenuPrice = restaurantMenuRepository.findMainMenuPricePerServing(restaurantId);

        if (mainMenuPrice == null) {
            log.debug("대표메뉴가 없는 가게: restaurantId={}", restaurantId);
            return null;
        }

        Long totalRestaurantCount = restaurantRepository.countRestaurantsWithMainMenu();
        Long cheaperRestaurantCount = restaurantRepository
                .countRestaurantsWithCheaperMainMenu(mainMenuPrice);

        return calculatePercentileLabel(cheaperRestaurantCount, totalRestaurantCount);
    }

    public Map<Long, Integer> calculateValueForMoneyPointsBatch(List<Long> restaurantIds) {
        if (restaurantIds == null || restaurantIds.isEmpty()) {
            log.debug("배치 만족도 계산 - restaurantIds가 비어있음");
            return Collections.emptyMap();
        }

        log.info("배치 만족도 계산 시작 - 가게 수: {}", restaurantIds.size());

        List<Object[]> results = restaurantRepository.findValueForMoneyScoresBatch(restaurantIds);
        log.info("배치 만족도 쿼리 결과 - 행 수: {}", results.size());

        Map<Long, List<Integer>> scoresByRestaurant = results.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (Integer) row[1], Collectors.toList())
                ));

        Map<Long, Integer> resultMap = scoresByRestaurant.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Integer> scores = entry.getValue();
                            if (scores.isEmpty()) return null;
                            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                            return (int) Math.round(avg);
                        }
                ));

        log.info("배치 만족도 계산 완료 - 결과 가게 수: {}", resultMap.size());
        return resultMap;
    }

    public Map<Long, String> calculatePricePercentilesBatch(List<Long> restaurantIds) {
        if (restaurantIds == null || restaurantIds.isEmpty()) {
            log.debug("배치 백분위수 계산 - restaurantIds가 비어있음");
            return Collections.emptyMap();
        }

        log.info("배치 백분위수 계산 시작 - 가게 수: {}", restaurantIds.size());

        List<Object[]> menuPrices = restaurantMenuRepository.findMainMenuPricesBatch(restaurantIds);
        log.info("배치 메뉴 가격 쿼리 결과 - 행 수: {}", menuPrices.size());

        if (menuPrices.isEmpty()) {
            log.warn("배치 백분위수 계산 실패 - 대표메뉴가 있는 가게 없음");
            return Collections.emptyMap();
        }

        menuPrices.forEach(row -> {
            Long restaurantId = (Long) row[0];
            Integer price = (Integer) row[1];
            log.debug("restaurantId={}, pricePerServing={}", restaurantId, price);
        });

        Long totalCount = restaurantRepository.countRestaurantsWithMainMenu();
        log.info("전체 대표메뉴 보유 가게 수: {}", totalCount);

        Set<Integer> uniquePrices = menuPrices.stream()
                .map(row -> (Integer) row[1])
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.info("고유 가격 종류 수: {}", uniquePrices.size());

        Map<Integer, Long> cheaperCountMap = new HashMap<>();
        for (Integer price : uniquePrices) {
            Long count = restaurantRepository.countRestaurantsWithCheaperMainMenu(price);
            cheaperCountMap.put(price, count);
            log.debug("가격 {}원보다 저렴한 가게 수: {}", price, count);
        }

        Map<Long, String> resultMap = menuPrices.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> {
                            Integer price = (Integer) row[1];
                            if (price == null) {
                                log.warn("가게 {}의 pricePerServing이 null", row[0]);
                                return null;
                            }
                            Long cheaperCount = cheaperCountMap.getOrDefault(price, 0L);
                            String percentile = calculatePercentileLabel(cheaperCount, totalCount);
                            log.debug("가게 {}: 가격={}원, 더 저렴한 가게 수={}, 백분위={}",
                                    row[0], price, cheaperCount, percentile);
                            return percentile;
                        }
                ));

        log.info("배치 백분위수 계산 완료 - 결과 가게 수: {}", resultMap.size());

        resultMap.forEach((id, percentile) -> {
            log.info("최종 결과 - restaurantId={}, percent={}", id, percentile);
        });

        return resultMap;
    }

    private String calculatePercentileLabel(Number lowerCount, Number totalCount) {
        if (totalCount == null || totalCount.longValue() == 0) {
            return null;
        }

        double percentile = (lowerCount.doubleValue() / totalCount.doubleValue()) * 100;

        if (percentile >= 70) {
            return "상위 30%";
        } else if (percentile >= 30) {
            return "평균 50%";
        } else {
            return "하위 30%";
        }
    }
}
