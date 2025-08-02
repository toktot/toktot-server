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
        log.atInfo()
                .setMessage("Starting review creation process")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", request.restaurantId())
                .addKeyValue("keywordCount", request.keywords().size())
                .addKeyValue("imageCount", request.images().size())
                .log();

        try {
            validateCreateRequest(request, user);

            Restaurant restaurant = findRestaurant(request.restaurantId());

            ReviewSessionDTO session = getValidSession(user.getId(), request.restaurantId());
            validateSessionImages(session, request.images());

            Review review = createReviewEntity(user, restaurant);

            reviewKeywordService.attachKeywords(review, request.keywords());

            processReviewImages(review, request.images(), session);

            Review savedReview = reviewRepository.save(review);

            reviewS3MigrationService.migrateSessionImages(session, savedReview.getId());

            reviewSessionService.deleteSession(user.getId(), request.restaurantId());

            log.atInfo()
                    .setMessage("Review creation completed successfully")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("reviewId", savedReview.getId())
                    .addKeyValue("restaurantId", request.restaurantId())
                    .addKeyValue("totalImages", savedReview.getImages().size())
                    .addKeyValue("totalKeywords", savedReview.getKeywords().size())
                    .log();

            return ReviewCreateResponse.from(
                    savedReview.getId(),
                    savedReview.getRestaurant().getId()
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Review creation failed - business error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", request.restaurantId())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .log();
            throw e;

        } catch (Exception e) {
            log.atError()
                    .setMessage("Review creation failed - system error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", request.restaurantId())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw new ToktotException(ErrorCode.SERVER_ERROR, "리뷰 저장에 실패했습니다.");
        }
    }

    private void validateCreateRequest(ReviewCreateRequest request, User user) {
        for (ReviewImageRequest imageRequest : request.images()) {
            if (imageRequest.tooltips() != null) {
                for (TooltipRequest tooltip : imageRequest.tooltips()) {
                    if (!tooltip.isValidFoodTooltip()) {
                        log.atWarn()
                                .setMessage("Invalid food tooltip data")
                                .addKeyValue("userId", user.getId())
                                .addKeyValue("imageId", imageRequest.imageId())
                                .addKeyValue("tooltipType", tooltip.type())
                                .log();
                        throw new ToktotException(ErrorCode.INVALID_INPUT,
                                "음식 툴팁에는 메뉴명, 가격, 인분이 필수입니다.");
                    }

                    if (!tooltip.isValidServiceTooltip()) {
                        log.atWarn()
                                .setMessage("Invalid service tooltip data")
                                .addKeyValue("userId", user.getId())
                                .addKeyValue("imageId", imageRequest.imageId())
                                .addKeyValue("tooltipType", tooltip.type())
                                .log();
                        throw new ToktotException(ErrorCode.INVALID_INPUT,
                                "서비스/청결 툴팁에는 별점만 입력 가능합니다.");
                    }
                }
            }
        }

        log.atDebug()
                .setMessage("Create request validation passed")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", request.restaurantId())
                .log();
    }

    private Restaurant findRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.atWarn()
                            .setMessage("Restaurant not found")
                            .addKeyValue("restaurantId", restaurantId)
                            .log();
                    return new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "음식점을 찾을 수 없습니다.");
                });
    }

    private ReviewSessionDTO getValidSession(Long userId, Long restaurantId) {
        ReviewSessionDTO session = reviewSessionService.getSession(userId, restaurantId)
                .orElseThrow(() -> {
                    log.atWarn()
                            .setMessage("Review session not found")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .log();
                    return new ToktotException(ErrorCode.RESOURCE_NOT_FOUND,
                            "이미지 업로드 세션을 찾을 수 없습니다. 이미지를 다시 업로드해주세요.");
                });

        if (session.getImages() == null || session.getImages().isEmpty()) {
            log.atWarn()
                    .setMessage("Empty session images")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .log();
            throw new ToktotException(ErrorCode.INVALID_INPUT, "업로드된 이미지가 없습니다.");
        }

        return session;
    }

    private void validateSessionImages(ReviewSessionDTO session, List<ReviewImageRequest> imageRequests) {
        Map<String, ReviewImageDTO> sessionImageMap = session.getImages().stream()
                .collect(Collectors.toMap(ReviewImageDTO::getImageId, Function.identity()));

        for (ReviewImageRequest imageRequest : imageRequests) {
            if (!sessionImageMap.containsKey(imageRequest.imageId())) {
                log.atWarn()
                        .setMessage("Image not found in session")
                        .addKeyValue("imageId", imageRequest.imageId())
                        .addKeyValue("sessionImageCount", session.getImages().size())
                        .log();
                throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND,
                        "업로드되지 않은 이미지가 포함되어 있습니다: " + imageRequest.imageId());
            }
        }

        log.atDebug()
                .setMessage("Session images validation passed")
                .addKeyValue("requestedImages", imageRequests.size())
                .addKeyValue("sessionImages", session.getImages().size())
                .log();
    }

    private Review createReviewEntity(User user, Restaurant restaurant) {
        Review review = Review.create(user, restaurant);

        log.atDebug()
                .setMessage("Review entity created")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", restaurant.getId())
                .log();

        return review;
    }

    private void processReviewImages(Review review, List<ReviewImageRequest> imageRequests,
                                     ReviewSessionDTO session) {
        Map<String, ReviewImageDTO> sessionImageMap = session.getImages().stream()
                .collect(Collectors.toMap(ReviewImageDTO::getImageId, Function.identity()));

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
                for (TooltipRequest tooltipRequest : imageRequest.tooltips()) {
                    Tooltip tooltip = createTooltipFromRequest(tooltipRequest);
                    reviewImage.addTooltip(tooltip);
                }
            }

            review.addImage(reviewImage);
        }

        log.atDebug()
                .setMessage("Review images processed")
                .addKeyValue("totalImages", imageRequests.size())
                .addKeyValue("totalTooltips", imageRequests.stream()
                        .mapToInt(img -> img.tooltips() != null ? img.tooltips().size() : 0)
                        .sum())
                .log();
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
