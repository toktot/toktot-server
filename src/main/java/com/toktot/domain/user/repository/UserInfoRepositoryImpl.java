package com.toktot.domain.user.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.toktot.domain.user.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.toktot.domain.review.QReview.review;
import static com.toktot.domain.review.QReviewImage.reviewImage;
import static com.toktot.domain.review.QTooltip.tooltip;
import static com.toktot.domain.user.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserInfoRepositoryImpl implements UserInfoRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<UserInfoResponse> findUserInfoWithReviewStats(Long userId) {
        UserInfoResponse result = queryFactory
                .select(Projections.constructor(UserInfoResponse.class,
                        user.nickname,
                        review.id.countDistinct(),
                        Expressions.numberTemplate(Double.class, "ROUND({0}, 1)", tooltip.rating.avg()).coalesce(0.0),
                        user.profileImageUrl))
                .from(user)
                .leftJoin(review).on(review.user.id.eq(user.id))
                .leftJoin(review.images, reviewImage)
                .leftJoin(reviewImage.tooltips, tooltip)
                .where(user.id.eq(userId).and(user.deletedAt.isNull()))
                .groupBy(user.id, user.nickname, user.profileImageUrl)
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
