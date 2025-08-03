package com.toktot.web.controller.review;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.review.service.ReviewService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.review.request.ReviewCreateRequest;
import com.toktot.web.dto.review.response.ReviewCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewCreateResponse>> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.atInfo()
                .setMessage("Review creation request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", request.restaurantId())
                .addKeyValue("keywordCount", request.keywords().size())
                .addKeyValue("imageCount", request.images().size())
                .addKeyValue("keywords", request.keywords().toArray())
                .log();

        try {
            ReviewCreateResponse response = reviewService.createReview(request, user);

            log.atInfo()
                    .setMessage("Review creation request completed successfully")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("reviewId", response.reviewId())
                    .addKeyValue("restaurantId", response.restaurantId())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success("리뷰가 등록되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Review creation request failed - business error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", request.restaurantId())
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("isValidationError", e.isValidationError())
                    .addKeyValue("isSystemError", e.isSystemError())
                    .log();

            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Review creation request failed - system error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", request.restaurantId())
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("exceptionType", e.getClass().getSimpleName())
                    .setCause(e)
                    .log();

            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }
}
