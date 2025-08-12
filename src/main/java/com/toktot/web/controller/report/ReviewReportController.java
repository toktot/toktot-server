package com.toktot.web.controller.report;

import com.toktot.domain.report.service.ReviewReportService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.report.request.ReviewReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/report/review")
@RequiredArgsConstructor
public class ReviewReportController {

    private final ReviewReportService reviewReportService;


    @GetMapping("/{reviewId}/can-report")
    public ResponseEntity<ApiResponse<Boolean>> canReportReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("리뷰 신고 가능 여부 확인")
                .addKeyValue("reporterId", user.getId())
                .addKeyValue("reviewId", reviewId)
                .log();

        boolean response = reviewReportService.canReportReview(user.getId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReviewReport(
            @Valid @RequestBody ReviewReportRequest request,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("리뷰 신고")
                .addKeyValue("reporterId", user.getId())
                .addKeyValue("reviewId", request.reviewId())
                .addKeyValue("reportTypes", request.reportTypes())
                .log();
        reviewReportService.reportReview(request, user);
        return ResponseEntity.ok(ApiResponse.success("신고 접수되었습니다."));
    }
}

