package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.review.type.TooltipType;
import com.toktot.domain.user.User;
import com.toktot.domain.review.dto.request.ReviewCreateRequest;
import com.toktot.domain.review.dto.request.ReviewImageRequest;
import com.toktot.domain.review.dto.request.TooltipRequest;
import com.toktot.domain.review.dto.response.create.ReviewCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewSessionService reviewSessionService;
    private final ReviewS3MigrationService reviewS3MigrationService;
    private final ReviewKeywordService reviewKeywordService;
    private final ReviewImageService reviewImageService;
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public ReviewCreateResponse createReview(ReviewCreateRequest request, User user) {
        for (ReviewImageRequest image : request.images()) {
            validateTooltipsInRequest(image.tooltips());
        }
        validateIsMain(request.images());
        validateImageOrder(request.images());
        Restaurant restaurant = restaurantRepository
                .findById(request.id())
                .orElseThrow(() -> new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND));
        ReviewSessionDTO reviewSessionDTO = getReviewSessionDTO(user.getId(), request.id());
        validateSessionImages(reviewSessionDTO, request.images());
        Review review = Review.create(user, restaurant, request.valueForMoneyScore(), request.mealTime());

        reviewKeywordService.saveKeywordsInReview(review, request.keywords());
        reviewRepository.save(review);
        reviewImageService.saveImagesInReview(review, request.images(), reviewSessionDTO);
        reviewS3MigrationService.migrateSessionImages(reviewSessionDTO, review.getId());

        reviewSessionService.deleteSession(user.getId(), request.id());
        return ReviewCreateResponse.from(review.getId(), review.getRestaurant().getId());
    }

    private void validateIsMain(List<ReviewImageRequest> requests) {
        int isMainCount = 0;
        for (ReviewImageRequest request : requests) {
            if (request.isMain()) {
                isMainCount++;
            }
        }

        if (isMainCount == 0) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "대표 이미지가 선택되지 않았습니다.");
        }
        if (isMainCount > 1) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "대표 이미지는 1개만 선택 가능합니다.");
        }
    }

    private void validateImageOrder(List<ReviewImageRequest> requests) {
        HashSet<Integer> orderSet = new HashSet<>();
        for (ReviewImageRequest image : requests) {
            if (image.order() != null && !orderSet.add(image.order())) {
                throw new ToktotException(ErrorCode.INVALID_INPUT, "이미지의 순서가 잘못되었습니다.");
            }
        }
    }

    private ReviewSessionDTO getReviewSessionDTO(Long userId, Long restaurantId) {
        ReviewSessionDTO session = reviewSessionService.getSession(userId, restaurantId)
                .orElseThrow(() -> {
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
                throw new ToktotException(ErrorCode.RESOURCE_NOT_FOUND,
                        "업로드되지 않은 이미지가 포함되어 있습니다: " + imageRequest.imageId());
            }
        }
    }

    private void validateTooltipsInRequest(List<TooltipRequest> requests) {
        if (requests != null && !requests.isEmpty()) {
            for (TooltipRequest request : requests) {
                if (request.type().equals(TooltipType.FOOD)
                        && (request.menuName() == null ||
                        request.menuName().trim().isEmpty() ||
                        request.totalPrice() == null)) {
                    throw new ToktotException(ErrorCode.INVALID_INPUT, "음식 툴팁에 메뉴명, 가격은 필수입니다.");
                }


                if (!request.type().equals(TooltipType.FOOD) &&
                        (request.menuName() != null ||
                        request.totalPrice() != null ||
                        request.servingSize() != null)) {
                    throw new ToktotException(ErrorCode.INVALID_INPUT, "서비스/청결 툴팁은 별점과 상세 리뷰만 입력 가능합니다.");
                }
            }
        }
    }
}
