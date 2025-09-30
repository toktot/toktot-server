package com.toktot.domain.review.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.toktot.domain.folder.QFolderReview;
import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.dto.response.search.*;
import com.toktot.domain.review.type.KeywordType;
import com.toktot.domain.review.type.TooltipType;
import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.request.SearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.toktot.domain.folder.QFolderReview.folderReview;
import static com.toktot.domain.restaurant.QRestaurant.restaurant;
import static com.toktot.domain.review.QReview.review;
import static com.toktot.domain.review.QReviewImage.reviewImage;
import static com.toktot.domain.review.QReviewKeyword.reviewKeyword;
import static com.toktot.domain.review.QTooltip.tooltip;
import static com.toktot.domain.user.QUser.user;

@Repository
@RequiredArgsConstructor
public class ReviewSearchRepositoryImpl implements ReviewSearchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ReviewListResponse> searchReviewsWithFilters(SearchCriteria criteria, Long currentUserId,
                                                             List<Long> blockedUserIds, Pageable pageable) {
        BooleanBuilder builder = buildCommonWhereClause(criteria, blockedUserIds);

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .join(review.user, user).fetchJoin()
                .join(review.restaurant, restaurant).fetchJoin()
                .leftJoin(review.images, reviewImage).fetchJoin()
                .where(builder)
                .orderBy(buildOrderSpecifiers(criteria))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<ReviewListResponse> content = convertToReviewListResponses(reviews, currentUserId);

        JPAQuery<Long> countQuery = queryFactory
                .select(review.count())
                .from(review)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<ReviewListResponse> findSavedReviews(Long userId, Long folderId,
                                                     List<Long> blockedUserIds, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!blockedUserIds.isEmpty()) {
            builder.and(review.user.id.notIn(blockedUserIds));
        }

        QFolderReview fr = folderReview;
        builder.and(JPAExpressions
                .selectOne()
                .from(fr)
                .where(fr.review.id.eq(review.id)
                        .and(fr.folder.user.id.eq(userId))
                        .and(folderId != null ? fr.folder.id.eq(folderId) : null))
                .exists());

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .join(review.user, user).fetchJoin()
                .join(review.restaurant, restaurant).fetchJoin()
                .leftJoin(review.images, reviewImage).fetchJoin()
                .where(builder)
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<ReviewListResponse> content = convertToReviewListResponses(reviews, userId);

        JPAQuery<Long> countQuery = queryFactory
                .select(review.count())
                .from(review)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<ReviewListResponse> findMyReviews(Long userId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(review.user.id.eq(userId));

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .join(review.user, user).fetchJoin()
                .join(review.restaurant, restaurant).fetchJoin()
                .leftJoin(review.images, reviewImage).fetchJoin()
                .where(builder)
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<ReviewListResponse> content = convertToReviewListResponses(reviews, userId);

        JPAQuery<Long> countQuery = queryFactory
                .select(review.count())
                .from(review)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<ReviewListResponse> findUserReviews(Long targetUserId, Long currentUserId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(review.user.id.eq(targetUserId));
        builder.and(review.isHidden.eq(false));

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .join(review.user, user).fetchJoin()
                .join(review.restaurant, restaurant).fetchJoin()
                .leftJoin(review.images, reviewImage).fetchJoin()
                .where(builder)
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<ReviewListResponse> content = convertToReviewListResponses(reviews, currentUserId);

        JPAQuery<Long> countQuery = queryFactory
                .select(review.count())
                .from(review)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<RestaurantDetailReviewResponse> findRestaurantReviews(Long restaurantId, Long reviewId,
                                                                      SortType sortType, Long currentUserId,
                                                                      List<Long> blockedUserIds, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(review.restaurant.id.eq(restaurantId));

        if (!blockedUserIds.isEmpty()) {
            builder.and(review.user.id.notIn(blockedUserIds));
        }

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .join(review.user, user).fetchJoin()
                .join(review.restaurant, restaurant).fetchJoin()
                .leftJoin(review.images, reviewImage).fetchJoin()
                .leftJoin(reviewImage.tooltips, tooltip).fetchJoin()
                .leftJoin(review.keywords, reviewKeyword).fetchJoin()
                .where(builder)
                .orderBy(buildRestaurantReviewOrder(sortType, reviewId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream().distinct().toList();

        List<RestaurantDetailReviewResponse> content = convertToRestaurantDetailResponses(reviews, currentUserId);

        JPAQuery<Long> countQuery = queryFactory
                .select(review.countDistinct())
                .from(review)
                .leftJoin(review.keywords, reviewKeyword)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<ReviewFeedResponse> findReviewFeed(SearchCriteria criteria, Long currentUserId,
                                                   List<Long> blockedUserIds, Pageable pageable) {
        BooleanBuilder builder = buildCommonWhereClause(criteria, blockedUserIds);

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .join(review.user, user).fetchJoin()
                .join(review.restaurant, restaurant).fetchJoin()
                .leftJoin(review.images, reviewImage).fetchJoin()
                .leftJoin(reviewImage.tooltips, tooltip).fetchJoin()
                .leftJoin(review.keywords, reviewKeyword).fetchJoin()
                .where(builder)
                .orderBy(buildOrderSpecifiers(criteria))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream().distinct().toList();

        List<ReviewFeedResponse> content = convertToReviewFeedResponses(reviews, currentUserId);

        JPAQuery<Long> countQuery = queryFactory
                .select(review.countDistinct())
                .from(review)
                .leftJoin(review.keywords, reviewKeyword)
                .leftJoin(review.images, reviewImage)
                .leftJoin(reviewImage.tooltips, tooltip)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public RestaurantReviewStatisticsResponse getRestaurantReviewStatistics(Long restaurantId) {
        Long totalCount = queryFactory
                .select(review.count())
                .from(review)
                .where(review.restaurant.id.eq(restaurantId))
                .fetchOne();
        totalCount = (totalCount == null) ? 0L : totalCount;

        Double overallAvgRatingDouble = queryFactory
                .select(tooltip.rating.avg())
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .where(review.restaurant.id.eq(restaurantId))
                .fetchOne();
        BigDecimal overallRating = (overallAvgRatingDouble == null) ? BigDecimal.ZERO : BigDecimal.valueOf(overallAvgRatingDouble);

        List<Tuple> tooltipRatings = queryFactory
                .select(tooltip.tooltipType, tooltip.rating.avg())
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .where(review.restaurant.id.eq(restaurantId))
                .groupBy(tooltip.tooltipType)
                .fetch();

        Map<TooltipType, BigDecimal> ratingMap = tooltipRatings.stream()
                .collect(Collectors.toMap(
                        row -> row.get(tooltip.tooltipType),
                        row -> {
                            Double avg = row.get(tooltip.rating.avg());
                            return (avg == null) ? BigDecimal.ZERO : BigDecimal.valueOf(avg);
                        }
                ));

        Tuple satisfactionTuple = queryFactory
                .select(
                        new CaseBuilder().when(review.valueForMoneyScore.goe(70)).then(1L).otherwise(0L).sum(),
                        new CaseBuilder().when(review.valueForMoneyScore.between(40, 69)).then(1L).otherwise(0L).sum(),
                        new CaseBuilder().when(review.valueForMoneyScore.lt(40)).then(1L).otherwise(0L).sum()
                )
                .from(review)
                .where(review.restaurant.id.eq(restaurantId))
                .fetchOne();

        long highCount = 0, midCount = 0, lowCount = 0;
        if (satisfactionTuple != null) {
            highCount = satisfactionTuple.get(0, Long.class) != null ? satisfactionTuple.get(0, Long.class) : 0L;
            midCount = satisfactionTuple.get(1, Long.class) != null ? satisfactionTuple.get(1, Long.class) : 0L;
            lowCount = satisfactionTuple.get(2, Long.class) != null ? satisfactionTuple.get(2, Long.class) : 0L;
        }

        double highRange = totalCount > 0 ? ((double) highCount / totalCount) * 100 : 0.0;
        double midRange = totalCount > 0 ? ((double) midCount / totalCount) * 100 : 0.0;
        double lowRange = totalCount > 0 ? ((double) lowCount / totalCount) * 100 : 0.0;

        return RestaurantReviewStatisticsResponse.from(
                totalCount.intValue(),
                overallRating.setScale(1, RoundingMode.HALF_UP),
                ratingMap.getOrDefault(TooltipType.FOOD, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP),
                ratingMap.getOrDefault(TooltipType.CLEAN, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP),
                ratingMap.getOrDefault(TooltipType.SERVICE, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP),
                highRange,
                midRange,
                lowRange
        );
    }

    @Override
    public Page<ReviewListResponse> searchLocalFoodReviewsWithPriceFilter(LocalFoodType localFoodType,
                                                                          Integer minPrice, Integer maxPrice,
                                                                          SearchCriteria criteria, Long currentUserId,
                                                                          List<Long> blockedUserIds, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(tooltip.tooltipType.eq(TooltipType.FOOD))
                .and(tooltip.menuName.isNotNull())
                .and(tooltip.totalPrice.isNotNull())
                .and(tooltip.servingSize.isNotNull())
                .and(tooltip.servingSize.gt(0))
                .and(tooltip.totalPrice.divide(tooltip.servingSize).between(minPrice, maxPrice));

        if (!blockedUserIds.isEmpty()) {
            builder.and(review.user.id.notIn(blockedUserIds));
        }

        if (criteria.hasLocationFilter()) {
            double radiusInDegrees = criteria.radius() / 111320.0;
            builder.and(restaurant.latitude.between(
                            criteria.latitude() - radiusInDegrees,
                            criteria.latitude() + radiusInDegrees))
                    .and(restaurant.longitude.between(
                            criteria.longitude() - radiusInDegrees,
                            criteria.longitude() + radiusInDegrees));
        }

        if (criteria.hasRatingFilter()) {
            builder.and(tooltip.rating.goe(BigDecimal.valueOf(criteria.minRating())));
        }

        if (criteria.hasMealTimeFilter()) {
            builder.and(review.mealTime.eq(criteria.mealTime()));
        }

        List<Review> reviews = queryFactory
                .select(review).distinct()
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .join(review.restaurant, restaurant)
                .join(review.user, user)
                .where(builder)
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(review.countDistinct())
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .join(review.restaurant, restaurant)
                .where(builder)
                .fetchOne();

        List<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toList());
        Set<Long> bookmarkedReviewIds = findBookmarkedReviewIds(reviewIds, currentUserId);

        return new PageImpl<>(
                reviews.stream().map(r -> {
                    ReviewRestaurantInfo restaurantInfo = ReviewRestaurantInfo.from(r.getRestaurant(), null);
                    boolean isBookmarked = bookmarkedReviewIds.contains(r.getId());
                    boolean isWriter = currentUserId != null && r.getUser().getId().equals(currentUserId);

                    return ReviewListResponse.from(r, restaurantInfo, isBookmarked, isWriter);
                }).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    private BooleanBuilder buildCommonWhereClause(SearchCriteria criteria, List<Long> blockedUserIds) {
        BooleanBuilder builder = new BooleanBuilder();

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

        if (criteria.hasLocalFoodFilter()) {
            builder.and(JPAExpressions
                    .selectOne()
                    .from(tooltip)
                    .where(tooltip.reviewImage.review.id.eq(review.id)
                            .and(tooltip.tooltipType.eq(TooltipType.FOOD))
                            .and(tooltip.menuName.contains(criteria.localFoodType().getDisplayName())))
                    .exists());
        }

        if (criteria.hasPriceRangeFilter()) {
            if (criteria.localFoodMinPrice() != null) {
                builder.and(JPAExpressions
                        .select(tooltip.totalPrice.sum())
                        .from(tooltip)
                        .where(tooltip.reviewImage.review.id.eq(review.id)
                                .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
                        .goe(criteria.localFoodMinPrice()));
            }
            if (criteria.localFoodMaxPrice() != null) {
                builder.and(JPAExpressions
                        .select(tooltip.totalPrice.sum())
                        .from(tooltip)
                        .where(tooltip.reviewImage.review.id.eq(review.id)
                                .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
                        .loe(criteria.localFoodMaxPrice()));
            }
        }

        if (criteria.hasMealTimeFilter()) {
            builder.and(review.mealTime.eq(criteria.mealTime()));
        }

        if (criteria.hasKeywordFilter()) {
            criteria.keywords().stream()
                    .map(String::toUpperCase)
                    .map(keywordStr -> {
                        try { return KeywordType.valueOf(keywordStr); }
                        catch (IllegalArgumentException e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .forEach(keywordType -> builder.and(JPAExpressions
                            .selectOne()
                            .from(reviewKeyword)
                            .where(reviewKeyword.review.id.eq(review.id)
                                    .and(reviewKeyword.keywordType.eq(keywordType)))
                            .exists()));
        }

        return builder;
    }

    private NumberExpression<Double> buildDistanceExpression(SearchCriteria criteria) {
        return Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians(restaurant.latitude)) * cos(radians(restaurant.longitude) - radians({1})) + sin(radians({0})) * sin(radians(restaurant.latitude)))",
                criteria.latitude(), criteria.longitude());
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(SearchCriteria criteria) {
        if (criteria.hasSortFilter()) {
            return switch (criteria.sort()) {
                case DISTANCE -> new OrderSpecifier[]{buildDistanceExpression(criteria).asc(), review.createdAt.desc()};
                case POPULARITY -> new OrderSpecifier[]{buildPopularityExpression().desc(), review.createdAt.desc()};
                case RATING -> new OrderSpecifier[]{buildRatingExpression().desc(), review.createdAt.desc()};
                case SATISFACTION -> new OrderSpecifier[]{review.valueForMoneyScore.desc(), review.createdAt.desc()};
            };
        }
        return new OrderSpecifier[]{review.createdAt.desc()};
    }

    private OrderSpecifier<?>[] buildRestaurantReviewOrder(SortType sortType, Long reviewId) {
        if (reviewId != null) {
            return new OrderSpecifier[]{
                    new CaseBuilder().when(review.id.eq(reviewId)).then(1).otherwise(0).desc(),
                    review.createdAt.desc()
            };
        }

        if (sortType != null) {
            return switch (sortType) {
                case POPULARITY -> new OrderSpecifier[]{buildPopularityExpression().desc(), review.createdAt.desc()};
                case RATING -> new OrderSpecifier[]{buildRatingExpression().desc(), review.createdAt.desc()};
                case SATISFACTION -> new OrderSpecifier[]{review.valueForMoneyScore.desc(), review.createdAt.desc()};
                default -> new OrderSpecifier[]{review.createdAt.desc()};
            };
        }
        return new OrderSpecifier[]{review.createdAt.desc()};
    }

    private NumberExpression<Long> buildPopularityExpression() {
        return Expressions.numberTemplate(Long.class, "({0})",
                JPAExpressions
                        .select(folderReview.count())
                        .from(folderReview)
                        .where(folderReview.review.eq(review))
        );
    }

    private NumberExpression<Double> buildRatingExpression() {
        return Expressions.numberTemplate(Double.class, "({0})",
                JPAExpressions
                        .select(tooltip.rating.avg())
                        .from(tooltip)
                        .where(tooltip.reviewImage.review.eq(review))
        );
    }

    private List<ReviewListResponse> convertToReviewListResponses(List<Review> reviews, Long currentUserId) {
        if (reviews.isEmpty()) return Collections.emptyList();

        Set<Long> bookmarkedReviewIds = findBookmarkedReviewIds(
                reviews.stream().map(Review::getId).toList(), currentUserId);

        return reviews.stream()
                .map(r -> {
                    ReviewRestaurantInfo restaurantInfo = ReviewRestaurantInfo.from(r.getRestaurant(), null);
                    boolean isBookmarked = bookmarkedReviewIds.contains(r.getId());
                    boolean isWriter = r.getUser().getId().equals(currentUserId);
                    return ReviewListResponse.from(r, restaurantInfo, isBookmarked, isWriter);
                })
                .toList();
    }

    private List<RestaurantDetailReviewResponse> convertToRestaurantDetailResponses(List<Review> reviews, Long currentUserId) {
        if (reviews.isEmpty()) return Collections.emptyList();

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        List<Long> userIds = reviews.stream().map(r -> r.getUser().getId()).distinct().toList();

        Set<Long> bookmarkedReviewIds = findBookmarkedReviewIds(reviewIds, currentUserId);
        Map<Long, BigDecimal> reviewRatings = findAverageRatingsForReviews(reviewIds);
        Map<Long, Long> userReviewCounts = findReviewCountsByUserIds(userIds);
        Map<Long, BigDecimal> userAverageRatings = findAverageRatingsByUserIds(userIds);

        return reviews.stream()
                .map(r -> {
                    com.toktot.domain.user.User authorUser = r.getUser();
                    long reviewCount = userReviewCounts.getOrDefault(authorUser.getId(), 0L);
                    BigDecimal avgRating = userAverageRatings.getOrDefault(authorUser.getId(), BigDecimal.ZERO);
                    ReviewAuthorResponse author = ReviewAuthorResponse.from(authorUser, (int) reviewCount, avgRating);

                    BigDecimal reviewRating = reviewRatings.getOrDefault(r.getId(), BigDecimal.ZERO);
                    Set<String> keywords = r.getKeywords().stream()
                            .map(k -> k.getKeywordType().getDisplayName())
                            .collect(Collectors.toSet());
                    boolean isBookmarked = bookmarkedReviewIds.contains(r.getId());
                    boolean isWriter = authorUser.getId().equals(currentUserId);

                    return RestaurantDetailReviewResponse.from(r, author, reviewRating, keywords, isBookmarked, isWriter);
                })
                .toList();
    }

    private List<ReviewFeedResponse> convertToReviewFeedResponses(List<Review> reviews, Long currentUserId) {
        if (reviews.isEmpty()) return Collections.emptyList();

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        List<Long> userIds = reviews.stream().map(r -> r.getUser().getId()).distinct().toList();

        Set<Long> bookmarkedReviewIds = findBookmarkedReviewIds(reviewIds, currentUserId);
        Map<Long, Long> userReviewCounts = findReviewCountsByUserIds(userIds);
        Map<Long, BigDecimal> userAverageRatings = findAverageRatingsByUserIds(userIds);

        return reviews.stream()
                .map(r -> {
                    com.toktot.domain.user.User authorUser = r.getUser();
                    long reviewCount = userReviewCounts.getOrDefault(authorUser.getId(), 0L);
                    BigDecimal avgRating = userAverageRatings.getOrDefault(authorUser.getId(), BigDecimal.ZERO);
                    ReviewAuthorResponse author = ReviewAuthorResponse.from(authorUser, (int) reviewCount, avgRating);

                    Set<String> keywords = r.getKeywords().stream()
                            .map(k -> k.getKeywordType().getDisplayName())
                            .collect(Collectors.toSet());
                    ReviewRestaurantInfo restaurantInfo = ReviewRestaurantInfo.from(r.getRestaurant(), null);
                    boolean isBookmarked = bookmarkedReviewIds.contains(r.getId());
                    boolean isWriter = authorUser.getId().equals(currentUserId);

                    return ReviewFeedResponse.from(r, author, keywords, restaurantInfo, isBookmarked, isWriter);
                })
                .toList();
    }

    private Set<Long> findBookmarkedReviewIds(List<Long> reviewIds, Long currentUserId) {
        if (currentUserId == null || reviewIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(queryFactory
                .select(folderReview.review.id)
                .from(folderReview)
                .where(folderReview.review.id.in(reviewIds)
                        .and(folderReview.folder.user.id.eq(currentUserId)))
                .fetch());
    }

    private Map<Long, Long> findReviewCountsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) return Collections.emptyMap();
        return queryFactory
                .select(user.id, review.count())
                .from(review)
                .join(review.user, user)
                .where(user.id.in(userIds))
                .groupBy(user.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(user.id),
                        t -> t.get(review.count())
                ));
    }

    private Map<Long, BigDecimal> findAverageRatingsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) return Collections.emptyMap();
        return queryFactory
                .select(user.id, tooltip.rating.avg())
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .join(review.user, user)
                .where(user.id.in(userIds))
                .groupBy(user.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(user.id),
                        t -> {
                            Double avg = t.get(tooltip.rating.avg());
                            return (avg == null) ? BigDecimal.ZERO : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
                        }
                ));
    }

    private Map<Long, BigDecimal> findAverageRatingsForReviews(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) return Collections.emptyMap();
        return queryFactory
                .select(review.id, tooltip.rating.avg())
                .from(tooltip)
                .join(tooltip.reviewImage, reviewImage)
                .join(reviewImage.review, review)
                .where(review.id.in(reviewIds))
                .groupBy(review.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(review.id),
                        t -> {
                            Double avg = t.get(tooltip.rating.avg());
                            return (avg == null) ? BigDecimal.ZERO : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
                        }
                ));
    }
}
