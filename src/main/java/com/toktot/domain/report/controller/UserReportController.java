package com.toktot.domain.report.controller;

import com.toktot.domain.report.service.UserReportService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.report.dto.UserReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/report/users")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;

    @GetMapping("/{reportedUserId}/can-report")
    public ResponseEntity<ApiResponse<Boolean>> canReportReview(
            @PathVariable Long reportedUserId,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("리뷰 신고 가능 여부 확인")
                .addKeyValue("reporterId", user.getId())
                .addKeyValue("reportedUserId", reportedUserId)
                .log();

        userReportService.canReportUser(user.getId(), reportedUserId);
        return ResponseEntity.ok(ApiResponse.success("신고 가능한 대상입니다."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUserReport(
            @Valid @RequestBody UserReportRequest request,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("리뷰 신고")
                .addKeyValue("reporterId", user.getId())
                .addKeyValue("reportedUserId", request.reportedUserId())
                .addKeyValue("reportTypes", request.reportTypes())
                .log();

        userReportService.reportUser(request, user);
        return ResponseEntity.ok(ApiResponse.success("신고가 접수되었습니다."));
    }

}
