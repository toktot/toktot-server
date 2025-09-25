package com.toktot.domain.restaurant.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.domain.restaurant.service.RestaurantStatisticsService;
import com.toktot.domain.review.QTooltip;
import com.toktot.domain.review.type.KeywordType;
import com.toktot.domain.review.type.TooltipType;
import com.toktot.web.dto.request.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.toktot.domain.folder.QFolderReview.folderReview;
import static com.toktot.domain.restaurant.QRestaurant.restaurant;
import static com.toktot.domain.review.QReview.review;
import static com.toktot.domain.review.QReviewImage.reviewImage;
import static com.toktot.domain.review.QReviewKeyword.reviewKeyword;
import static com.toktot.domain.review.QTooltip.tooltip;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RestaurantSearchRepositoryImpl implements RestaurantSearchRepository {

    private final JPAQueryFactory queryFactory;
    private final RestaurantStatisticsService restaurantStatisticsService;

    @Override
    public Page<RestaurantInfoResponse> searchRestaurantsWithFilters(SearchCriteria criteria,
                                                                     Long currentUserId,
                                                                     List<Long> blockedUserIds,
                                                                     Pageable pageable) {

        BooleanBuilder reviewFilterBuilder = buildReviewFilterConditions(criteria, blockedUserIds);

        List<Long> restaurantIds = queryFactory
                .select(restaurant.id).distinct()
                .from(review)
                .join(review.restaurant, restaurant)
                .where(reviewFilterBuilder)
                .fetch();

        if (restaurantIds.isEmpty()) {
            return Page.empty(pageable);
        }

        BooleanBuilder restaurantBuilder = new BooleanBuilder();
        restaurantBuilder.and(restaurant.id.in(restaurantIds))
                .and(restaurant.isActive.isTrue());

        if (criteria.hasLocationFilter()) {
            restaurantBuilder.and(buildDistanceExpression(criteria).loe(criteria.radius().doubleValue()));
        }

        OrderSpecifier<?>[] orderSpecifiers = buildRestaurantOrderSpecifiers(criteria);

        List<Restaurant> restaurants = queryFactory
                .selectFrom(restaurant)
                .where(restaurantBuilder)
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(restaurant.count())
                .from(restaurant)
                .where(restaurantBuilder)
                .fetchOne();

        return new PageImpl<>(
                restaurants.stream()
                        .map(r -> convertToRestaurantInfoResponse(r, criteria))
                        .collect(Collectors.toList()),
                pageable,
                Objects.requireNonNullElse(total, 0L)
        );
    }

    @Override
    public Page<RestaurantInfoResponse> searchRestaurantsByIds(List<Long> restaurantIds,
                                                               SearchCriteria criteria,
                                                               Long currentUserId,
                                                               List<Long> blockedUserIds,
                                                               Pageable pageable) {

        if (restaurantIds.isEmpty()) {
            return Page.empty(pageable);
        }

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(restaurant.id.in(restaurantIds))
                .and(restaurant.isActive.isTrue());

        if (criteria.hasLocationFilter()) {
            builder.and(buildDistanceExpression(criteria).loe(criteria.radius().doubleValue()));
        }

        if (!blockedUserIds.isEmpty()) {
            List<Long> restaurantsWithBlockedUsers = queryFactory
                    .select(restaurant.id).distinct()
                    .from(review)
                    .join(review.restaurant, restaurant)
                    .where(review.user.id.in(blockedUserIds))
                    .fetch();

            if (!restaurantsWithBlockedUsers.isEmpty()) {
                builder.and(restaurant.id.notIn(restaurantsWithBlockedUsers));
            }
        }

        OrderSpecifier<?>[] orderSpecifiers = buildRestaurantOrderSpecifiers(criteria);

        List<Restaurant> restaurants = queryFactory
                .selectFrom(restaurant)
                .where(builder)
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(restaurant.count())
                .from(restaurant)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(
                restaurants.stream()
                        .map(r -> convertToRestaurantInfoResponse(r, criteria))
                        .collect(Collectors.toList()),
                pageable,
                total
        );
    }

    private BooleanBuilder buildReviewFilterConditions(SearchCriteria criteria, List<Long> blockedUserIds) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(review.isHidden.isFalse());

        if (blockedUserIds != null && !blockedUserIds.isEmpty()) {
            builder.and(review.user.id.notIn(blockedUserIds));
        }

        if (criteria.hasValidQuery()) {
            builder.and(
                    restaurant.name.containsIgnoreCase(criteria.query())
                            .or(JPAExpressions
                                    .selectOne()
                                    .from(tooltip)
                                    .where(tooltip.reviewImage.review.id.eq(review.id)
                                            .and(tooltip.menuName.containsIgnoreCase(criteria.query())))
                                    .exists())
            );
        }

        if (criteria.hasLocationFilter()) {
            builder.and(buildDistanceExpression(criteria).loe(criteria.radius().doubleValue()));
        }

        if (criteria.hasRatingFilter()) {
            builder.and(JPAExpressions
                    .select(tooltip.rating.avg())
                    .from(tooltip)
                    .where(tooltip.reviewImage.review.id.eq(review.id)
                            .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
                    .goe(criteria.minRating()));
        }

        if (criteria.hasMealTimeFilter()) {
            builder.and(review.mealTime.eq(criteria.mealTime()));
        }

        if (criteria.hasKeywordFilter()) {
            for (String keyword : criteria.keywords()) {
                try {
                    KeywordType keywordType = KeywordType.valueOf(keyword.toUpperCase());
                    builder.and(JPAExpressions
                            .selectOne()
                            .from(reviewKeyword)
                            .where(reviewKeyword.review.id.eq(review.id)
                                    .and(reviewKeyword.keywordType.eq(keywordType)))
                            .exists());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid keyword type: {}", keyword);
                    continue;
                }
            }
        }

        return builder;
    }

    private NumberExpression<Double> buildDistanceExpression(SearchCriteria criteria) {
        return Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1})))",
                criteria.latitude(),
                restaurant.latitude,
                restaurant.longitude,
                criteria.longitude());
    }

    private OrderSpecifier<?>[] buildRestaurantOrderSpecifiers(SearchCriteria criteria) {
        if (criteria.sort() != null) {
            return switch (criteria.sort()) {
                case DISTANCE -> criteria.hasLocationFilter() ?
                        new OrderSpecifier[]{buildDistanceExpression(criteria).asc(), restaurant.createdAt.desc()} :
                        new OrderSpecifier[]{restaurant.createdAt.desc()};
                case RATING -> new OrderSpecifier[]{
                        buildRestaurantAvgRatingExpression().desc().nullsLast(),
                        restaurant.createdAt.desc()
                };
                case POPULARITY -> new OrderSpecifier[]{
                        buildRestaurantPopularityExpression().desc(),
                        restaurant.createdAt.desc()
                };
                case SATISFACTION -> new OrderSpecifier[]{
                        buildRestaurantSatisfactionExpression().desc().nullsLast(),
                        restaurant.createdAt.desc()
                };
            };
        }
        return new OrderSpecifier[]{restaurant.createdAt.desc()};
    }

    private NumberExpression<Long> buildRestaurantPopularityExpression() {
        return Expressions.numberTemplate(Long.class, "({0})",
                JPAExpressions
                        .select(folderReview.count())
                        .from(folderReview)
                        .join(folderReview.review, review)
                        .where(review.restaurant.id.eq(restaurant.id))
        );
    }

    private NumberExpression<Double> buildRestaurantAvgRatingExpression() {
        return Expressions.numberTemplate(Double.class, "({0})",
                JPAExpressions
                        .select(tooltip.rating.avg())
                        .from(tooltip)
                        .join(tooltip.reviewImage, reviewImage)
                        .join(reviewImage.review, review)
                        .where(review.restaurant.id.eq(restaurant.id)
                                .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
        );
    }

    private NumberExpression<Double> buildRestaurantSatisfactionExpression() {
        return Expressions.numberTemplate(Double.class, "({0})",
                JPAExpressions
                        .select(review.valueForMoneyScore.avg())
                        .from(review)
                        .where(review.restaurant.id.eq(restaurant.id))
        );
    }

    private RestaurantInfoResponse convertToRestaurantInfoResponse(Restaurant restaurant, SearchCriteria criteria) {
        String distance = null;
        if (criteria.hasLocationFilter()) {
            Double distanceKm = calculateDistance(
                    criteria.latitude(), criteria.longitude(),
                    restaurant.getLatitude(), restaurant.getLongitude()
            );
            distance = distanceKm != null ? String.format("%.1fkm", distanceKm) : null;
        }

        BigDecimal avgRating = restaurantStatisticsService.calculateAverageRating(restaurant.getId());
        Long reviewCount = restaurantStatisticsService.calculateReviewCount(restaurant.getId());
        Integer valueForMoneyPoint = restaurantStatisticsService.calculateValueForMoneyPoint(restaurant.getId());
        String pricePercentile = restaurantStatisticsService.calculatePricePercentile(restaurant.getId());

        return RestaurantInfoResponse.withStatsComplete(restaurant, null, avgRating,
                reviewCount, distance, valueForMoneyPoint, pricePercentile);
    }

    private Double calculateDistance(Double lat1, Double lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        double lat2Double = lat2.doubleValue();
        double lon2Double = lon2.doubleValue();

        double earthRadius = 6371;
        double latDistance = Math.toRadians(lat2Double - lat1);
        double lonDistance = Math.toRadians(lon2Double - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2Double)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}
