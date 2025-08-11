package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.web.dto.review.response.ReviewResponse;
import com.toktot.web.dto.user.ReviewUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantReviewService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public Page<ReviewResponse> getRestaurantReviews(Long restaurantId, Pageable pageable, Long userId) {
        log.info("가게 리뷰 조회 시작 - restaurantId: {}, page: {}, size: {}",
                restaurantId, pageable.getPageNumber(), pageable.getPageSize());

        validateRestaurantExists(restaurantId);

        Page<Review> reviewPage = reviewRepository.findByRestaurantIdWithDetails(restaurantId, pageable);

        log.info("리뷰 조회 완료 - 총 {}개 중 {}개 조회",
                reviewPage.getTotalElements(), reviewPage.getNumberOfElements());

        return reviewPage.map(review -> convertToReviewResponse(review, userId));
    }

    private void validateRestaurantExists(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            log.warn("존재하지 않는 가게 ID: {}", restaurantId);
            throw new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND);
        }
    }

    private ReviewResponse convertToReviewResponse(Review review, Long userId) {
        return ReviewResponse.from(review, userId, getReviewUserResponse(review.getUser()));
    }

    private ReviewUserResponse getReviewUserResponse(User user) {
        Integer reviewCount = userRepository.countReviewsByUserId(user.getId());
        BigDecimal averageRating = userRepository.calculateAverageRatingByUserId(user.getId());

        return ReviewUserResponse.from(user, reviewCount, averageRating);
    }
}
