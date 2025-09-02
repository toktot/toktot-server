package com.toktot.domain.review.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.toktot.domain.review.dto.response.ReviewSearchResponse;
import com.toktot.domain.review.type.TooltipType;
import com.toktot.web.dto.request.SearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    public Page<ReviewSearchResponse> searchReviewsWithFilters(SearchCriteria criteria, Pageable pageable) {
        BooleanBuilder builder = buildWhereClause(criteria);

        JPAQuery<ReviewSearchResponse> query = queryFactory
                .select(Projections.constructor(ReviewSearchResponse.class,
                        review.id,
                        JPAExpressions
                                .select(reviewImage.imageUrl)
                                .from(reviewImage)
                                .where(reviewImage.review.eq(review)
                                        .and(reviewImage.imageOrder.eq(1)))
                                .limit(1),
                        user.nickname,
                        user.profileImageUrl,
                        review.createdAt,
                        Expressions.stringTemplate(
                                "CASE " +
                                        "WHEN {0} LIKE '%제주시%' THEN CONCAT('제주시 ', SPLIT_PART({0}, ' ', 3)) " +
                                        "WHEN {0} LIKE '%서귀포시%' THEN CONCAT('서귀포시 ', SPLIT_PART({0}, ' ', 3)) " +
                                        "ELSE SPLIT_PART({0}, ' ', 1) || ' ' || SPLIT_PART({0}, ' ', 2) " +
                                        "END",
                                restaurant.address
                        ),
                        buildDistanceExpression(criteria)
                ))
                .from(review)
                .join(review.user, user)
                .join(review.restaurant, restaurant)
                .where(builder.and(review.isHidden.eq(false)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        addOrderBy(query, pageable, criteria);

        List<ReviewSearchResponse> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(review.count())
                .from(review)
                .join(review.restaurant, restaurant)
                .where(builder.and(review.isHidden.eq(false)));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder buildWhereClause(SearchCriteria criteria) {
        BooleanBuilder builder = new BooleanBuilder();

        if (criteria.hasValidQuery()) {
            BooleanBuilder queryBuilder = new BooleanBuilder();
            queryBuilder.or(restaurant.name.containsIgnoreCase(criteria.query()));
            queryBuilder.or(JPAExpressions
                    .selectOne()
                    .from(tooltip)
                    .where(tooltip.reviewImage.review.eq(review)
                            .and(tooltip.menuName.containsIgnoreCase(criteria.query())))
                    .exists());
            builder.and(queryBuilder);
        }

        if (criteria.hasLocationFilter()) {
            NumberExpression<Double> distance = buildDistanceExpression(criteria);
            builder.and(distance.loe(criteria.radius().doubleValue()));
        }

        if (criteria.hasRatingFilter()) {
            builder.and(JPAExpressions
                    .select(tooltip.rating.avg())
                    .from(tooltip)
                    .where(tooltip.reviewImage.review.eq(review)
                            .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
                    .goe(criteria.minRating()));
        }

        if (criteria.hasLocalFoodFilter()) {
            builder.and(JPAExpressions
                    .selectOne()
                    .from(tooltip)
                    .where(tooltip.reviewImage.review.eq(review)
                            .and(tooltip.tooltipType.eq(TooltipType.FOOD))
                            .and(tooltip.menuName.contains(criteria.localFoodType().getDisplayName())))
                    .exists());
        }

        if (criteria.hasPriceRangeFilter()) {
            if (criteria.localFoodMinPrice() != null) {
                builder.and(JPAExpressions
                        .select(tooltip.totalPrice.sum())
                        .from(tooltip)
                        .where(tooltip.reviewImage.review.eq(review)
                                .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
                        .goe(criteria.localFoodMinPrice()));
            }
            if (criteria.localFoodMaxPrice() != null) {
                builder.and(JPAExpressions
                        .select(tooltip.totalPrice.sum())
                        .from(tooltip)
                        .where(tooltip.reviewImage.review.eq(review)
                                .and(tooltip.tooltipType.eq(TooltipType.FOOD)))
                        .loe(criteria.localFoodMaxPrice()));
            }
        }

        if (criteria.hasMealTimeFilter()) {
            builder.and(review.mealTime.eq(criteria.mealTime()));
        }

        if (criteria.hasKeywordFilter()) {
            for (String keyword : criteria.keywords()) {
                builder.and(JPAExpressions
                        .selectOne()
                        .from(reviewKeyword)
                        .where(reviewKeyword.review.eq(review)
                                .and(reviewKeyword.keywordType.stringValue().containsIgnoreCase(keyword)))
                        .exists());
            }
        }

        return builder;
    }

    private NumberExpression<Double> buildDistanceExpression(SearchCriteria criteria) {
        if (!criteria.hasLocationFilter()) {
            return Expressions.numberTemplate(Double.class, "0.0");
        }

        return Expressions.numberTemplate(Double.class,
                "6371000 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1})))",
                criteria.latitude(),
                restaurant.latitude,
                restaurant.longitude,
                criteria.longitude()
        );
    }

    private void addOrderBy(JPAQuery<ReviewSearchResponse> query, Pageable pageable, SearchCriteria criteria) {
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

                switch (order.getProperty()) {
                    case "createdAt":
                        query.orderBy(new OrderSpecifier<>(direction, review.createdAt));
                        break;
                    case "rating":
                        query.orderBy(new OrderSpecifier<>(direction,
                                JPAExpressions
                                        .select(tooltip.rating.avg())
                                        .from(tooltip)
                                        .where(tooltip.reviewImage.review.eq(review))
                        ));
                        break;
                    case "valueForMoneyScore":
                        query.orderBy(new OrderSpecifier<>(direction, review.valueForMoneyScore));
                        break;
                    case "distance":
                        if (criteria.hasLocationFilter()) {
                            query.orderBy(new OrderSpecifier<>(direction, buildDistanceExpression(criteria)));
                        } else {
                            query.orderBy(new OrderSpecifier<>(Order.DESC, review.createdAt));
                        }
                        break;
                    case "popularity":
                        query.orderBy(new OrderSpecifier<>(direction,
                                JPAExpressions
                                        .select(review.count())
                                        .from(review)
                                        .where(review.restaurant.eq(review.restaurant))
                        ));
                        break;
                    default:
                        query.orderBy(new OrderSpecifier<>(Order.DESC, review.createdAt));
                        break;
                }
            }
        } else {
            query.orderBy(review.createdAt.desc());
        }
    }
}
