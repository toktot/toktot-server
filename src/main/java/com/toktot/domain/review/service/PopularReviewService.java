package com.toktot.domain.review.service;

import com.toktot.domain.folder.repository.FolderReviewRepository;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.ReviewImage;
import com.toktot.domain.review.dto.response.search.PopularReviewResponse;
import com.toktot.domain.review.dto.response.search.ReviewAuthorResponse;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.review.repository.TooltipRepository;
import com.toktot.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    public List<PopularReviewResponse> getPopularReviews() {
        log.info("많이 저장한 리뷰 조회");

        List<Long> popularReviewIds = folderReviewRepository.findPopularReviewIds(PageRequest.of(0, 15));
        if (popularReviewIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Review> reviews = reviewRepository.findWithDetailsByIds(popularReviewIds);
        Set<Long> userIds = reviews.stream()
                .map(review -> review.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, Double> averageRatingsByReviewId = getAverageRatingsByReviewId(popularReviewIds);
        Map<Long, Long> reviewCountsByUserId = getReviewCountsByUserId(userIds);
        Map<Long, BigDecimal> averageRatingsByUserId = getAverageRatingsByUserId(userIds);

        return reviews.stream()
                .filter(review -> !review.getIsHidden())
                .map(review -> createPopularReviewResponse(review, averageRatingsByReviewId, reviewCountsByUserId, averageRatingsByUserId))
                .toList();
    }

    private PopularReviewResponse createPopularReviewResponse(Review review,
                                                              Map<Long, Double> avgRatingsByReview,
                                                              Map<Long, Long> reviewCountsByUser,
                                                              Map<Long, BigDecimal> avgRatingsByUser) {

        User user = review.getUser();
        ReviewAuthorResponse authorResponse = ReviewAuthorResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .reviewCount(reviewCountsByUser.getOrDefault(user.getId(), 0L).intValue())
                .averageRating(avgRatingsByUser.get(user.getId()))
                .build();

        return PopularReviewResponse.builder()
                .id(review.getId())
                .author(authorResponse)
                .valueForMoneyScore(review.getValueForMoneyScore())
                .keywords(review.getKeywords())
                .imageUrl(getMainImageUrl(review.getImages()))
                .rating(avgRatingsByReview.get(review.getId()))
                .build();
    }

    private String getMainImageUrl(Set<ReviewImage> images) {
        return images.stream()
                .filter(ReviewImage::getIsMain)
                .findFirst()
                .map(ReviewImage::getImageUrl)
                .orElse(null);
    }

    private Map<Long, Double> getAverageRatingsByReviewId(List<Long> reviewIds) {
        return tooltipRepository.findAverageRatingsByReviewIds(reviewIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }

    private Map<Long, Long> getReviewCountsByUserId(Set<Long> userIds) {
        return reviewRepository.findReviewCountsByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Map<Long, BigDecimal> getAverageRatingsByUserId(Set<Long> userIds) {
        return tooltipRepository.findAverageRatingsByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> BigDecimal.valueOf((Double) row[1])
                ));
    }
}

