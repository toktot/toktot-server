package com.toktot.domain.review.service;

import com.toktot.domain.block.UserBlockRepository;
import com.toktot.domain.review.dto.response.search.RestaurantDetailReviewResponse;
import com.toktot.domain.review.dto.response.search.RestaurantReviewStatisticsResponse;
import com.toktot.domain.review.dto.response.search.ReviewFeedResponse;
import com.toktot.domain.review.dto.response.search.ReviewListResponse;
import com.toktot.domain.review.repository.ReviewSearchRepositoryCustom;
import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.request.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewSearchService {

    private final ReviewSearchRepositoryCustom reviewSearchRepository;
    private final UserBlockRepository userBlockRepository;

    public Page<ReviewListResponse> searchReviews(SearchCriteria criteria, Long currentUserId, Pageable pageable) {
        log.info("리뷰 검색 시작 - query: {}, userId: {}", criteria.query(), currentUserId);

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        return reviewSearchRepository.searchReviewsWithFilters(criteria, currentUserId, blockedUserIds, pageable);
    }

    public Page<ReviewListResponse> searchLocalFoodReviewsWithPriceFilter(SearchCriteria criteria,
                                                                          Long currentUserId,
                                                                          Pageable pageable) {
        log.info("향토음식 가격 필터링 검색 시작 - localFoodType: {}, minPrice: {}, maxPrice: {}",
                criteria.localFoodType(), criteria.localFoodMinPrice(), criteria.localFoodMaxPrice());

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        return reviewSearchRepository.searchLocalFoodReviewsWithPriceFilter(
                criteria.localFoodType(),
                criteria.localFoodMinPrice(),
                criteria.localFoodMaxPrice(),
                criteria,
                currentUserId,
                blockedUserIds,
                pageable
        );
    }

    public Page<ReviewListResponse> getSavedReviews(Long userId, Long folderId, Pageable pageable) {
        log.info("저장한 리뷰 조회 - userId: {}, folderId: {}", userId, folderId);

        List<Long> blockedUserIds = getBlockedUserIds(userId);

        return reviewSearchRepository.findSavedReviews(userId, folderId, blockedUserIds, pageable);
    }

    public Page<ReviewListResponse> getMyReviews(Long userId, Pageable pageable) {
        log.info("작성한 리뷰 조회 - userId: {}", userId);

        return reviewSearchRepository.findMyReviews(userId, pageable);
    }

    public Page<RestaurantDetailReviewResponse> getRestaurantReviews(Long restaurantId, Long reviewId,
                                                                     SortType sortType, Long currentUserId,
                                                                     Pageable pageable) {
        log.info("가게 상세 페이지 리뷰 조회 - restaurantId: {}, reviewId: {}, sortType: {}",
                restaurantId, reviewId, sortType);

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        return reviewSearchRepository.findRestaurantReviews(restaurantId, reviewId, sortType,
                currentUserId, blockedUserIds, pageable);
    }

    public Page<ReviewFeedResponse> getReviewFeed(SearchCriteria criteria, Long currentUserId, Pageable pageable) {
        log.info("실시간 리뷰 피드 조회 - userId: {}", currentUserId);

        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);

        return reviewSearchRepository.findReviewFeed(criteria, currentUserId, blockedUserIds, pageable);
    }

    public RestaurantReviewStatisticsResponse getRestaurantReviewStatistics(Long restaurantId) {
        log.info("가게 리뷰 통계 조회 - restaurantId: {}", restaurantId);

        return reviewSearchRepository.getRestaurantReviewStatistics(restaurantId);
    }

    private List<Long> getBlockedUserIds(Long currentUserId) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        return userBlockRepository.findBlockedUserIdsByBlockerUserId(currentUserId);
    }
}
