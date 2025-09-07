package com.toktot.domain.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.domain.block.UserBlockRepository;
import com.toktot.domain.folder.repository.FolderReviewRepository;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.ReviewImage;
import com.toktot.domain.review.dto.response.search.PopularReviewResponse;
import com.toktot.domain.review.dto.response.search.ReviewAuthorResponse;
import com.toktot.domain.review.dto.response.search.ReviewRestaurantInfo;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.review.repository.TooltipRepository;
import com.toktot.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularReviewService {

    private final FolderReviewRepository folderReviewRepository;
    private final ReviewRepository reviewRepository;
    private final TooltipRepository tooltipRepository;
    private final UserBlockRepository userBlockRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String POPULAR_REVIEWS_KEY = "popular:reviews";
    private static final int POPULAR_REVIEW_COUNT = 15;

    public List<PopularReviewResponse> getPopularReviewsForUser(Long currentUserId) {
        log.info("사용자별 인기 리뷰 조회 시작 - userId: {}", currentUserId);

        List<PopularReviewResponse> cachedReviews = getCachedPopularReviews();

        if (cachedReviews.isEmpty()) {
            log.warn("캐시된 인기 리뷰가 없어 DB에서 직접 조회합니다.");
            return fetchAndMapPopularReviews(currentUserId);
        }

        return applyUserContextToCachedReviews(cachedReviews, currentUserId);
    }

    public List<PopularReviewResponse> getPopularReviews() {
        log.info("스케줄러용 인기 리뷰 조회 시작");
        return fetchAndMapPopularReviews(null);
    }

    private List<PopularReviewResponse> applyUserContextToCachedReviews(List<PopularReviewResponse> cachedReviews, Long currentUserId) {
        List<Long> blockedUserIds = getBlockedUserIds(currentUserId);
        List<PopularReviewResponse> filteredReviews = cachedReviews.stream()
                .filter(review -> !blockedUserIds.contains(review.author().id()))
                .toList();

        if (filteredReviews.isEmpty()) {
            return Collections.emptyList();
        }

        List<PopularReviewResponse> finalReviews = updateBookmarkStatus(filteredReviews, currentUserId);

        log.info("캐시 기반 사용자별 리뷰 필터링 완료 - 원본: {}개, 최종: {}개", cachedReviews.size(), finalReviews.size());
        return finalReviews;
    }

    private List<PopularReviewResponse> fetchAndMapPopularReviews(Long currentUserId) {
        List<Long> popularReviewIds = folderReviewRepository.findPopularReviewIds(PageRequest.of(0, POPULAR_REVIEW_COUNT));
        if (popularReviewIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Review> reviews = reviewRepository.findWithDetailsByIds(popularReviewIds);
        Set<Long> userIds = reviews.stream()
                .map(review -> review.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, Double> averageRatingsByReviewId = getAverageRatingsByReviewId(popularReviewIds);
        Map<Long, Long> reviewCountsByUserId = getReviewCountsByUserId(userIds);
        Map<Long, BigDecimal> averageRatingsByUserId = getAverageRatingsByUserId(userIds);
        Set<Long> bookmarkedReviewIds = getBookmarkedReviewIds(popularReviewIds, currentUserId);

        return reviews.stream()
                .filter(review -> !review.getIsHidden())
                .map(review -> createPopularReviewResponse(
                        review,
                        averageRatingsByReviewId,
                        reviewCountsByUserId,
                        averageRatingsByUserId,
                        bookmarkedReviewIds.contains(review.getId())
                ))
                .toList();
    }

    private List<PopularReviewResponse> getCachedPopularReviews() {
        try {
            String cachedJson = redisTemplate.opsForValue().get(POPULAR_REVIEWS_KEY);
            if (cachedJson == null || cachedJson.isBlank()) {
                log.debug("캐시된 인기 리뷰가 없습니다.");
                return Collections.emptyList();
            }
            List<PopularReviewResponse> reviews = objectMapper.readValue(cachedJson, new TypeReference<>() {});
            log.debug("캐시에서 인기 리뷰 {}개 조회", reviews.size());
            return reviews;
        } catch (JsonProcessingException e) {
            log.error("캐시된 인기 리뷰 역직렬화 실패. 캐시를 비우고 DB 조회를 유도합니다.", e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("캐시 조회 중 예기치 않은 오류 발생", e);
            return Collections.emptyList();
        }
    }

    private List<PopularReviewResponse> updateBookmarkStatus(List<PopularReviewResponse> reviews, Long currentUserId) {
        if (currentUserId == null) {
            return reviews.stream()
                    .map(review -> review.withIsBookmarked(false))
                    .toList();
        }

        List<Long> reviewIds = reviews.stream().map(PopularReviewResponse::id).toList();
        Set<Long> bookmarkedReviewIds = getBookmarkedReviewIds(reviewIds, currentUserId);

        return reviews.stream()
                .map(review -> review.withIsBookmarked(bookmarkedReviewIds.contains(review.id())))
                .toList();
    }

    private List<Long> getBlockedUserIds(Long currentUserId) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }
        return userBlockRepository.findBlockedUserIdsByBlockerUserId(currentUserId);
    }

    private Set<Long> getBookmarkedReviewIds(List<Long> reviewIds, Long currentUserId) {
        if (currentUserId == null || reviewIds.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            return Set.copyOf(folderReviewRepository.findBookmarkedReviewIds(reviewIds, currentUserId));
        } catch (Exception e) {
            log.error("북마크 상태 조회 실패 - userId: {}", currentUserId, e);
            return Collections.emptySet();
        }
    }

    private PopularReviewResponse createPopularReviewResponse(Review review,
                                                              Map<Long, Double> avgRatingsByReview,
                                                              Map<Long, Long> reviewCountsByUser,
                                                              Map<Long, BigDecimal> avgRatingsByUser,
                                                              boolean isBookmarked) {
        User user = review.getUser();
        ReviewAuthorResponse authorResponse = ReviewAuthorResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .reviewCount(reviewCountsByUser.getOrDefault(user.getId(), 0L).intValue())
                .averageRating(avgRatingsByUser.get(user.getId()))
                .build();

        List<String> keywords = review.getKeywords().stream()
                .map(rk -> rk.getKeywordType().getDisplayName())
                .toList();

        ReviewRestaurantInfo restaurantInfo = ReviewRestaurantInfo.from(review.getRestaurant(), null);

        return PopularReviewResponse.builder()
                .id(review.getId())
                .author(authorResponse)
                .isBookmarked(isBookmarked)
                .valueForMoneyScore(review.getValueForMoneyScore())
                .keywords(keywords)
                .imageUrl(getMainImageUrl(review.getImages()))
                .restaurant(restaurantInfo)
                .rating(avgRatingsByReview.get(review.getId()))
                .build();
    }

    private String getMainImageUrl(Set<ReviewImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ReviewImage::getIsMain)
                .findFirst()
                .map(ReviewImage::getImageUrl)
                .orElse(null);
    }

    private Map<Long, Double> getAverageRatingsByReviewId(List<Long> reviewIds) {
        return tooltipRepository.findAverageRatingsByReviewIds(reviewIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Double) row[1]));
    }

    private Map<Long, Long> getReviewCountsByUserId(Set<Long> userIds) {
        return reviewRepository.findReviewCountsByUserIds(userIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private Map<Long, BigDecimal> getAverageRatingsByUserId(Set<Long> userIds) {
        return tooltipRepository.findAverageRatingsByUserIds(userIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> BigDecimal.valueOf((Double) row[1])));
    }
}
