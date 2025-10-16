package com.toktot.domain.restaurant.repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.dto.response.PriceRangeRestaurantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.toktot.domain.restaurant.QRestaurant.restaurant;
import static com.toktot.domain.review.QReview.review;
import static com.toktot.domain.review.QReviewImage.reviewImage;
import static com.toktot.domain.review.QTooltip.tooltip;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RestaurantPriceRangeRepository {

    private final JPAQueryFactory queryFactory;

    public Page<PriceRangeRestaurantResponse> findRestaurantsByPriceRange(
            LocalFoodType localFoodType,
            Integer minPrice,
            Integer maxPrice,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radiusInMeters,
            Pageable pageable
    ) {
        log.info("가격대별 가게 조회 시작 - 향토음식: {}, 가격범위: {}~{}, 반경: {}m",
                localFoodType.getDisplayName(), minPrice, maxPrice, radiusInMeters);

        JPAQuery<Restaurant> query = queryFactory
                .selectFrom(restaurant)
                .where(
                        JPAExpressions
                                .selectOne()
                                .from(review)
                                .join(reviewImage).on(reviewImage.review.eq(review))
                                .join(tooltip).on(tooltip.reviewImage.eq(reviewImage))
                                .where(review.restaurant.id.eq(restaurant.id)
                                        .and(tooltip.menuName.containsIgnoreCase(localFoodType.getDisplayName()))
                                        .and(tooltip.totalPrice.isNotNull())
                                        .and(tooltip.servingSize.isNotNull())
                                        .and(tooltip.servingSize.gt(0))
                                        .and(tooltip.totalPrice.divide(tooltip.servingSize)
                                                .between(minPrice, maxPrice)))
                                .exists(),
                        restaurant.isActive.isTrue()
                );

        if (latitude != null && longitude != null) {
            NumberExpression<Double> distanceExpression = buildDistanceExpression(
                    latitude.doubleValue(),
                    longitude.doubleValue()
            );
            query.where(distanceExpression.loe(radiusInMeters / 1000.0));
            query.orderBy(distanceExpression.asc());
        } else {
            query.orderBy(restaurant.id.desc());
        }

        Long total = queryFactory
                .select(restaurant.count())
                .from(restaurant)
                .where(
                        JPAExpressions
                                .selectOne()
                                .from(review)
                                .join(reviewImage).on(reviewImage.review.eq(review))
                                .join(tooltip).on(tooltip.reviewImage.eq(reviewImage))
                                .where(review.restaurant.id.eq(restaurant.id)
                                        .and(tooltip.menuName.containsIgnoreCase(localFoodType.getDisplayName()))
                                        .and(tooltip.totalPrice.isNotNull())
                                        .and(tooltip.servingSize.isNotNull())
                                        .and(tooltip.servingSize.gt(0))
                                        .and(tooltip.totalPrice.divide(tooltip.servingSize)
                                                .between(minPrice, maxPrice)))
                                .exists(),
                        restaurant.isActive.isTrue()
                )
                .fetchOne();

        List<Restaurant> restaurants = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<PriceRangeRestaurantResponse> results = restaurants.stream()
                .map(r -> convertToResponse(r, latitude, longitude, minPrice, maxPrice))
                .collect(Collectors.toList());

        log.info("가격대별 가게 조회 완료 - 총 {}개 가게", total);

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    private PriceRangeRestaurantResponse convertToResponse(
            Restaurant restaurant,
            BigDecimal userLat,
            BigDecimal userLon,
            Integer minPrice,
            Integer maxPrice
    ) {
        Double avgRating = queryFactory
                .select(tooltip.rating.avg())
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .where(review.restaurant.id.eq(restaurant.getId())
                        .and(tooltip.rating.isNotNull()))
                .fetchOne();

        Long reviewCount = queryFactory
                .select(review.count())
                .from(review)
                .where(review.restaurant.id.eq(restaurant.getId()))
                .fetchOne();

        String imageUrl = queryFactory
                .select(reviewImage.imageUrl)
                .from(reviewImage)
                .join(reviewImage.review, review)
                .where(review.restaurant.id.eq(restaurant.getId())
                        .and(reviewImage.isMain.isTrue()))
                .orderBy(reviewImage.review.createdAt.desc())
                .fetchFirst();

        Double distance = null;
        if (userLat != null && userLon != null) {
            distance = calculateDistance(
                    userLat.doubleValue(),
                    userLon.doubleValue(),
                    restaurant.getLatitude().doubleValue(),
                    restaurant.getLongitude().doubleValue()
            );
        }

        return PriceRangeRestaurantResponse.builder()
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .distance(distance)
                .category(restaurant.getCategory())
                .averageRating(avgRating)
                .reviewCount(reviewCount != null ? reviewCount.intValue() : 0)
                .isGoodPriceStore(restaurant.getIsGoodPriceStore())
                .ImageUrl(imageUrl)
                .averagePriceInRange((minPrice + maxPrice) / 2)
                .build();
    }

    private NumberExpression<Double> buildDistanceExpression(double userLat, double userLon) {
        return Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians({1})) * " +
                        "cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1})))",
                userLat, restaurant.latitude, restaurant.longitude, userLon);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
