package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.common.util.DistanceCalculator;
import com.toktot.common.util.PriceParserUtil;
import com.toktot.domain.restaurant.dto.cache.GoodPriceRestaurantDto;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodPriceRestaurantService {

    private final GoodPriceCacheService goodPriceCacheService;

    public Page<RestaurantInfoResponse> getGoodPriceRestaurants(
            Integer priceRange,
            BigDecimal longitude,
            BigDecimal latitude,
            Pageable pageable) {

        Integer validPriceRange = validateAndNormalizePriceRange(priceRange);

        log.info("착한가격업소 조회 - 가격대: {}, 위치: ({}, {}), 페이지: {}",
                PriceParserUtil.getPriceRangeName(validPriceRange),
                latitude, longitude, pageable.getPageNumber());

        List<GoodPriceRestaurantDto> cachedRestaurants =
                goodPriceCacheService.getGoodPriceRestaurantsByPriceRange(validPriceRange);

        if (cachedRestaurants.isEmpty()) {
            log.warn("해당 가격대에 착한가격업소가 없습니다: {}",
                    PriceParserUtil.getPriceRangeName(validPriceRange));
            return Page.empty(pageable);
        }

        List<RestaurantInfoResponse> restaurants = cachedRestaurants.stream()
                .map(dto -> convertToResponseWithDistance(dto, longitude, latitude))
                .collect(Collectors.toList());

        return createPagedResult(restaurants, pageable);
    }

    private Integer validateAndNormalizePriceRange(Integer priceRange) {
        if (priceRange == null) {
            return 0;
        }

        if (!PriceParserUtil.isValidPriceRange(priceRange)) {
            throw new ToktotException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 가격대입니다: " + priceRange);
        }

        return priceRange;
    }

    private RestaurantInfoResponse convertToResponseWithDistance(
            GoodPriceRestaurantDto dto,
            BigDecimal userLongitude,
            BigDecimal userLatitude) {

        String distance = null;

        if (userLongitude != null && userLatitude != null &&
                dto.longitude() != null && dto.latitude() != null) {

            try {
                double distanceKm = DistanceCalculator.calculateDistance(
                        userLatitude.doubleValue(),
                        userLongitude.doubleValue(),
                        dto.latitude().doubleValue(),
                        dto.longitude().doubleValue()
                );

                distance = formatDistance(distanceKm);

            } catch (Exception e) {
                log.debug("거리 계산 실패: 매장 ID {}", dto.id(), e);
            }
        }

        return dto.toRestaurantInfoResponse(distance);
    }

    private String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            return String.format("%.0fm", distanceKm * 1000);
        } else {
            return String.format("%.1fkm", distanceKm);
        }
    }

    private Page<RestaurantInfoResponse> createPagedResult(
            List<RestaurantInfoResponse> allRestaurants,
            Pageable pageable) {

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allRestaurants.size());

        List<RestaurantInfoResponse> pageContent =
                (start < allRestaurants.size()) ? allRestaurants.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, allRestaurants.size());
    }

}
