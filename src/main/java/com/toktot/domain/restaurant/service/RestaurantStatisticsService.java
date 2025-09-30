package com.toktot.domain.restaurant.service;

import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.review.Tooltip;
import com.toktot.domain.review.repository.TooltipRepository;
import com.toktot.domain.review.type.TooltipType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantStatisticsService {

    private final RestaurantRepository restaurantRepository;
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
        List<Integer> valueForMoneyScores = restaurantRepository.findValueForMoneyScoresByRestaurantId(restaurantId);

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
        Integer representativeMenuPrice = getRepresentativeMenuPrice(restaurantId);

        if (representativeMenuPrice == null) {
            return null;
        }

        Integer totalRestaurantCount = restaurantRepository.countActiveRestaurants();
        Integer cheaperRestaurantCount = restaurantRepository.countRestaurantsWithCheaperRepresentativeMenu(representativeMenuPrice);

        if (totalRestaurantCount == null || totalRestaurantCount == 0) {
            return null;
        }

        double percentile = ((double) cheaperRestaurantCount / totalRestaurantCount) * 100;

        if (percentile >= 70) {
            return "상위 30%";
        } else if (percentile >= 30) {
            return "평균 50%";
        } else {
            return "하위 30%";
        }
    }

    private Integer getRepresentativeMenuPrice(Long restaurantId) {
        Optional<String> popularMenus = restaurantRepository.findPopularMenusByRestaurantId(restaurantId);
        if (popularMenus.isPresent() && !popularMenus.get().isEmpty()) {
            Integer extractedPrice = extractPriceFromPopularMenus(popularMenus.get());
            if (extractedPrice != null) {
                return extractedPrice;
            }
        }

        List<Integer> prices = restaurantRepository.findMostReviewedMenuPricesByRestaurantId(
                restaurantId,
                PageRequest.of(0, 1)
        );
        return prices.isEmpty() ? null : prices.get(0);
    }

    private Integer extractPriceFromPopularMenus(String popularMenus) {
        if (popularMenus == null || popularMenus.trim().isEmpty()) {
            return null;
        }

        String[] prices = popularMenus.split("[^0-9,]+");
        for (String priceStr : prices) {
            try {
                String cleanPrice = priceStr.replaceAll("[^0-9]", "");
                if (!cleanPrice.isEmpty()) {
                    int price = Integer.parseInt(cleanPrice);
                    if (price >= 1000 && price <= 100000) {
                        return price;
                    }
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }
        return null;
    }
}
