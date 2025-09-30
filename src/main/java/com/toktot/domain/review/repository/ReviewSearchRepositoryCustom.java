package com.toktot.domain.review.repository;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.review.dto.response.search.RestaurantDetailReviewResponse;
import com.toktot.domain.review.dto.response.search.RestaurantReviewStatisticsResponse;
import com.toktot.domain.review.dto.response.search.ReviewFeedResponse;
import com.toktot.domain.review.dto.response.search.ReviewListResponse;
import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.request.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewSearchRepositoryCustom {

    Page<ReviewListResponse> searchReviewsWithFilters(SearchCriteria criteria, Long currentUserId,
                                                      List<Long> blockedUserIds, Pageable pageable);

    Page<ReviewListResponse> findSavedReviews(Long userId, Long folderId,
                                              List<Long> blockedUserIds, Pageable pageable);

    Page<ReviewListResponse> findMyReviews(Long userId, Pageable pageable);

    Page<RestaurantDetailReviewResponse> findRestaurantReviews(Long restaurantId, Long reviewId,
                                                               SortType sortType, Long currentUserId,
                                                               List<Long> blockedUserIds, Pageable pageable);

    Page<ReviewFeedResponse> findReviewFeed(SearchCriteria criteria, Long currentUserId,
                                            List<Long> blockedUserIds, Pageable pageable);

    RestaurantReviewStatisticsResponse getRestaurantReviewStatistics(Long restaurantId);


    Page<ReviewListResponse> searchLocalFoodReviewsWithPriceFilter(
            LocalFoodType localFoodType,
            Integer minPrice,
            Integer maxPrice,
            SearchCriteria criteria,
            Long currentUserId,
            List<Long> blockedUserIds,
            Pageable pageable);

    Page<ReviewListResponse> findUserReviews(Long targetUserId, Long currentUserId, Pageable pageable);
}
