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
import java.util.List;

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
