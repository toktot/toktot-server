package com.toktot.web.controller.review;

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
        log.debug("Review creation request - userId: {}, restaurant.id: {}, keywordCount: {}, imageCount: {}",
                user.getId(), request.id(), request.keywords().size(), request.images().size());

        ReviewCreateResponse response = reviewService.createReview(request, user);

        return ResponseEntity.ok(ApiResponse.success("리뷰가 등록되었습니다.", response));
    }
}
