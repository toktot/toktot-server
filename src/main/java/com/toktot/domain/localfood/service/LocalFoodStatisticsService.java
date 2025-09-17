package com.toktot.domain.localfood.service;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.localfood.dto.LocalFoodStatsResponse;
import com.toktot.domain.review.Tooltip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalFoodStatisticsService {

    private final LocalFoodDetectionService detectionService;

    private static final int MIN_REVIEW_COUNT = 5;
    private static final int PRICE_RANGE_COUNT = 8;

    public LocalFoodStatsResponse calculatePriceStats(LocalFoodType localFoodType) {
        log.info("향토음식 통계 계산 시작 - 타입: {}", localFoodType.getDisplayName());

        List<Tooltip> tooltips = detectionService.findTooltipsByType(localFoodType);

        if (tooltips.size() < MIN_REVIEW_COUNT) {
            log.warn("리뷰 데이터 부족 - 타입: {}, 개수: {}", localFoodType.getDisplayName(), tooltips.size());
            return createInsufficientDataResponse(localFoodType);
        }

        List<Integer> pricesPerServing = tooltips.stream()
                .map(this::calculatePricePerServing)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (pricesPerServing.size() < MIN_REVIEW_COUNT) {
            log.warn("유효 가격 데이터 부족 - 타입: {}, 개수: {}", localFoodType.getDisplayName(), pricesPerServing.size());
            return createInsufficientDataResponse(localFoodType);
        }

        int averagePrice = calculateAverage(pricesPerServing);
        int minPrice = pricesPerServing.get(0);
        int maxPrice = pricesPerServing.get(pricesPerServing.size() - 1);

        LocalFoodStatsResponse.PriceDistribution distribution =
                calculatePriceDistribution(pricesPerServing, averagePrice);

        List<LocalFoodStatsResponse.PriceRangeData> rangeData =
                generatePriceRanges(pricesPerServing, minPrice, maxPrice);

        log.info("향토음식 통계 계산 완료 - 타입: {}, 리뷰: {}개, 평균가격: {}원",
                localFoodType.getDisplayName(), tooltips.size(), averagePrice);

        return LocalFoodStatsResponse.builder()
                .localFoodType(localFoodType)
                .displayName(localFoodType.getDisplayName())
                .totalReviewCount(tooltips.size())
                .averagePrice(averagePrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .priceDistribution(distribution)
                .priceRanges(rangeData)
                .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .hasSufficientData(true)
                .build();
    }

    private LocalFoodStatsResponse createInsufficientDataResponse(LocalFoodType localFoodType) {
        return LocalFoodStatsResponse.builder()
                .localFoodType(localFoodType)
                .displayName(localFoodType.getDisplayName())
                .totalReviewCount(0)
                .averagePrice(0)
                .minPrice(0)
                .maxPrice(0)
                .priceDistribution(null)
                .priceRanges(List.of())
                .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .hasSufficientData(false)
                .build();
    }

    private Integer calculatePricePerServing(Tooltip tooltip) {
        Integer totalPrice = tooltip.getTotalPrice();
        Integer servingSize = tooltip.getServingSize();

        if (totalPrice == null || servingSize == null || servingSize <= 0) {
            return null;
        }

        return totalPrice / servingSize;
    }

    private int calculateAverage(List<Integer> prices) {
        return (int) prices.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
    }

    private LocalFoodStatsResponse.PriceDistribution calculatePriceDistribution(
            List<Integer> prices, int averagePrice) {

        int cheapThreshold = (int) (averagePrice * 0.8);
        int expensiveThreshold = (int) (averagePrice * 1.2);

        int cheapCount = 0;
        int normalCount = 0;
        int expensiveCount = 0;

        for (int price : prices) {
            if (price <= cheapThreshold) {
                cheapCount++;
            } else if (price <= expensiveThreshold) {
                normalCount++;
            } else {
                expensiveCount++;
            }
        }

        int totalCount = prices.size();

        return LocalFoodStatsResponse.PriceDistribution.builder()
                .cheapCount(cheapCount)
                .normalCount(normalCount)
                .expensiveCount(expensiveCount)
                .cheapRatio(totalCount > 0 ? (double) cheapCount / totalCount : 0.0)
                .normalRatio(totalCount > 0 ? (double) normalCount / totalCount : 0.0)
                .expensiveRatio(totalCount > 0 ? (double) expensiveCount / totalCount : 0.0)
                .build();
    }

    private List<LocalFoodStatsResponse.PriceRangeData> generatePriceRanges(
            List<Integer> prices, int minPrice, int maxPrice) {

        if (prices.isEmpty()) {
            return List.of();
        }

        List<LocalFoodStatsResponse.PriceRangeData> ranges = new ArrayList<>();
        int priceGap = Math.max((maxPrice - minPrice) / PRICE_RANGE_COUNT, 1000);

        for (int i = 0; i < PRICE_RANGE_COUNT; i++) {
            int rangeMin = minPrice + (i * priceGap);
            int rangeMax = (i == PRICE_RANGE_COUNT - 1) ? maxPrice : rangeMin + priceGap - 1;

            long reviewCount = prices.stream()
                    .filter(price -> price >= rangeMin && price <= rangeMax)
                    .count();

            if (reviewCount > 0 || i < 3) {
                ranges.add(LocalFoodStatsResponse.PriceRangeData.builder()
                        .minPrice(rangeMin)
                        .maxPrice(rangeMax)
                        .reviewCount((int) reviewCount)
                        .label(formatPriceRangeLabel(rangeMin, rangeMax))
                        .build());
            }
        }

        return ranges;
    }

    private String formatPriceRangeLabel(int minPrice, int maxPrice) {
        NumberFormat formatter = NumberFormat.getInstance();

        if (minPrice == maxPrice) {
            return formatter.format(minPrice) + "원";
        }

        return formatter.format(minPrice) + "~" + formatter.format(maxPrice) + "원";
    }
}
