package com.toktot.web.controller;

import com.toktot.domain.review.dto.response.search.PopularReviewResponse;
import com.toktot.domain.review.service.PopularReviewService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final PopularReviewService popularReviewService;

    @GetMapping("/popular-reviews")
    public ResponseEntity<ApiResponse<List<PopularReviewResponse>>> getPopularReviews(
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("홈 화면 인기 리뷰 조회")
                .addKeyValue("userId", user != null ? user.getId() : null)
                .log();

        List<PopularReviewResponse> popularReviews = popularReviewService.getPopularReviewsForUser(
                user != null ? user.getId() : null
        );

        return ResponseEntity.ok(ApiResponse.success(popularReviews));
    }
}
