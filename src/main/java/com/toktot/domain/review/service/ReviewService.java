package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.review.*;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.user.User;
import com.toktot.web.dto.review.request.ReviewCreateRequest;
import com.toktot.web.dto.review.request.ReviewImageRequest;
import com.toktot.web.dto.review.request.TooltipRequest;
import com.toktot.web.dto.review.response.ReviewCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewKeywordService reviewKeywordService;
    private final ReviewSessionService reviewSessionService;
    private final ReviewS3MigrationService reviewS3MigrationService;

    public ReviewCreateResponse createReview(ReviewCreateRequest request, User user) {
        log.debug("Review creation started - userId: {}, externalKakaoId: {}, keywordCount: {}, imageCount: {}",
                user.getId(), request.externalKakaoId(), request.keywords().size(), request.images().size());

        validateCreateRequest(request, user);

        Restaurant restaurant = findRestaurant(request.externalKakaoId());

        ReviewSessionDTO session = getValidSession(user.getId(), request.externalKakaoId());
        validateSessionImages(session, request.images());

        Review review = createReviewEntity(user, restaurant);

        reviewKeywordService.attachKeywords(review, request.keywords());

        processReviewImages(review, request.images(), session);

        Review savedReview = reviewRepository.save(review);

        reviewS3MigrationService.migrateSessionImages(session, savedReview.getId());

        reviewSessionService.deleteSession(user.getId(), request.externalKakaoId());

        return ReviewCreateResponse.from(
                savedReview.getId(),
                savedReview.getRestaurant().getId()
        );
    }

    private void validateCreateRequest(ReviewCreateRequest request, User user) {
        for (ReviewImageRequest imageRequest : request.images()) {
            if (imageRequest.tooltips() != null) {
                for (TooltipRequest tooltip : imageRequest.tooltips()) {
                    if (!tooltip.isValidFoodTooltip()) {
                        log.warn("Invalid food tooltip - userId: {}, imageId: {}, type: {}",
                                user.getId(), imageRequest.imageId(), tooltip.type());
                        throw new ToktotException(ErrorCode.INVALID_INPUT,
                                "음식 툴팁에는 메뉴명, 가격, 인분이 필수입니다.");
                    }

                    if (!tooltip.isValidServiceTooltip()) {
                        log.warn("Invalid service tooltip - userId: {}, imageId: {}, type: {}",
                                user.getId(), imageRequest.imageId(), tooltip.type());
                        throw new ToktotException(ErrorCode.INVALID_INPUT,
                                "서비스/청결 툴팁에는 별점만 입력 가능합니다.");
                    }
                }
            }
        }

        log.debug("Create request validation passed - userId: {}, externalKakaoId: {}",
                user.getId(), request.externalKakaoId());
    }

    private Restaurant findRestaurant(Long externalKakaoId) {
        return restaurantRepository.findById(externalKakaoId)
                .orElseThrow(() -> {
                    log.warn("Restaurant not found - externalKakaoId: {}", externalKakaoId);
                    return new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "음식점을 찾을 수 없습니다.");
                });
    }

    private ReviewSessionDTO getValidSession(Long userId, Long restaurantId) {
        ReviewSessionDTO session = reviewSessionService.getSession(userId, restaurantId)
                .orElseThrow(() -> {
                    log.warn("Review session not found - userId: {}, restaurantId: {}", userId, restaurantId);
                    return new ToktotException(ErrorCode.RESOURCE_NOT_FOUND,
                            "이미지 업로드 세션을 찾을 수 없습니다. 이미지를 다시 업로드해주세요.");
                });

        if (session.getImages() == null || session.getImages().isEmpty()) {
            log.warn("Empty session images - userId: {}, restaurantId: {}", userId, restaurantId);
            throw new ToktotException(ErrorCode.INVALID_INPUT, "업로드된 이미지가 없습니다.");
        }

        return session;
    }

    private void validateSessionImages(ReviewSessionDTO session, List<ReviewImageRequest> imageRequests) {
        Map<String, ReviewImageDTO> sessionImageMap = session.getImages().stream()
                .collect(Collectors.toMap(ReviewImageDTO::getImageId, Function.identity()));

        for (ReviewImageRequest imageRequest : imageRequests) {
            if (!sessionImageMap.containsKey(imageRequest.imageId())) {
                log.warn("Image not found in session - imageId: {}, sessionImageCount: {}",
                        imageRequest.imageId(), session.getImages().size());
                throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND,
                        "업로드되지 않은 이미지가 포함되어 있습니다: " + imageRequest.imageId());
            }
        }

        log.debug("Session images validation passed - requestedImages: {}, sessionImages: {}",
                imageRequests.size(), session.getImages().size());
    }

    private Review createReviewEntity(User user, Restaurant restaurant) {
        Review review = Review.create(user, restaurant);

        log.debug("Review entity created - userId: {}, restaurantId: {}", user.getId(), restaurant.getId());

        return review;
    }

    private void processReviewImages(Review review, List<ReviewImageRequest> imageRequests,
                                     ReviewSessionDTO session) {
        Map<String, ReviewImageDTO> sessionImageMap = session.getImages().stream()
                .collect(Collectors.toMap(ReviewImageDTO::getImageId, Function.identity()));

        int totalTooltips = 0;
        for (ReviewImageRequest imageRequest : imageRequests) {
            ReviewImageDTO sessionImage = sessionImageMap.get(imageRequest.imageId());

            ReviewImage reviewImage = ReviewImage.create(
                    sessionImage.getImageId(),
                    sessionImage.getS3Key(),
                    sessionImage.getImageUrl(),
                    sessionImage.getFileSize(),
                    imageRequest.order()
            );

            if (imageRequest.tooltips() != null) {
                totalTooltips += imageRequest.tooltips().size();
                for (TooltipRequest tooltipRequest : imageRequest.tooltips()) {
                    Tooltip tooltip = createTooltipFromRequest(tooltipRequest);
                    reviewImage.addTooltip(tooltip);
                }
            }

            review.addImage(reviewImage);
        }

        log.debug("Review images processed - totalImages: {}, totalTooltips: {}",
                imageRequests.size(), totalTooltips);
    }

    private Tooltip createTooltipFromRequest(TooltipRequest request) {
        return switch (request.type()) {
            case FOOD -> Tooltip.createFoodTooltip(
                    request.xPosition(),
                    request.yPosition(),
                    request.menuName(),
                    request.totalPrice(),
                    request.servingSize(),
                    request.rating(),
                    request.detailedReview()
            );
            case SERVICE, CLEAN -> Tooltip.createServiceTooltip(
                    request.type(),
                    request.xPosition(),
                    request.yPosition(),
                    request.rating()
            );
        };
    }
}
