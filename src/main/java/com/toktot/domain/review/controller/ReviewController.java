package com.toktot.domain.review.controller;

import com.toktot.domain.review.dto.response.search.ReviewFeedResponse;
import com.toktot.domain.review.dto.response.search.ReviewListResponse;
import com.toktot.domain.review.service.ReviewFilterService;
import com.toktot.domain.review.service.ReviewSearchService;
import com.toktot.domain.review.service.ReviewService;
import com.toktot.domain.search.dto.response.EnhancedSearchResponse;
import com.toktot.domain.search.service.EnhancedSearchService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.review.dto.request.ReviewCreateRequest;
import com.toktot.domain.review.dto.response.create.ReviewCreateResponse;
import com.toktot.web.dto.request.SearchCriteria;
import com.toktot.web.dto.request.SearchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewSearchService reviewSearchService;
    private final ReviewFilterService reviewFilterService;
    private final EnhancedSearchService enhancedSearchService;

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

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<EnhancedSearchResponse<Page<ReviewListResponse>>>> searchReviews(
            @Valid @RequestBody SearchRequest request,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.atInfo()
                .setMessage("리뷰 검색 요청")
                .addKeyValue("query", request.query())
                .addKeyValue("localFoodFilter", request.hasLocalFoodFilter())
                .addKeyValue("priceFilter", request.hasPriceRangeFilter())
                .addKeyValue("userId", user != null ? user.getId() : null)
                .log();

        SearchCriteria criteria = reviewFilterService.validateAndConvert(request);

        Page<ReviewListResponse> reviewResults;

        if (request.hasLocalFoodFilter() && request.hasPriceRangeFilter()) {
            reviewResults = reviewSearchService.searchLocalFoodReviewsWithPriceFilter(
                    criteria,
                    user != null ? user.getId() : null,
                    pageable);
        } else {
            reviewResults = reviewSearchService.searchReviews(
                    criteria,
                    user != null ? user.getId() : null,
                    pageable);
        }

        EnhancedSearchResponse<Page<ReviewListResponse>> response =
                enhancedSearchService.enhanceWithLocalFoodStats(request, reviewResults);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/feed")
    public ResponseEntity<ApiResponse<Page<ReviewFeedResponse>>> getReviewFeed(
            @RequestBody(required = false) SearchRequest request,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.atInfo()
                .setMessage("실시간 리뷰 피드 요청")
                .addKeyValue("userId", user != null ? user.getId() : null)
                .log();

        SearchCriteria criteria = request != null ?
                reviewFilterService.validateAndConvert(request) :
                new SearchCriteria(null, null, null, null, null, null, null, null, null, null, null);

        Page<ReviewFeedResponse> response = reviewSearchService.getReviewFeed(
                criteria,
                user != null ? user.getId() : null,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ReviewListResponse>>> getMyReviews(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.atInfo()
                .setMessage("작성한 리뷰 조회 요청")
                .addKeyValue("userId", user.getId())
                .log();

        Page<ReviewListResponse> response = reviewSearchService.getMyReviews(user.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Page<ReviewListResponse>>> getUserReviews(
            @PathVariable @Positive Long userId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.atInfo()
                .setMessage("특정 사용자 리뷰 조회 요청")
                .addKeyValue("targetUserId", userId)
                .addKeyValue("currentUserId", currentUser != null ? currentUser.getId() : null)
                .log();

        Page<ReviewListResponse> response = reviewSearchService.getUserReviews(
                userId,
                currentUser != null ? currentUser.getId() : null,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long reviewId) {
        log.info("delete review - reviewId: {}", reviewId);

        reviewService.deleteReview(user.getId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다."));
    }
}
