package com.toktot.domain.user.controller;

import com.toktot.common.util.ClientInfoExtractor;
import com.toktot.domain.user.User;
import com.toktot.domain.user.service.AuditLogService;
import com.toktot.domain.user.service.AuthService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class LogoutController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @AuthenticationPrincipal User user,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("로그아웃 요청 - userId: {}, email: {}", user.getId(), user.getEmail());

        String clientIp = ClientInfoExtractor.getClientIp(request);
        String userAgent = ClientInfoExtractor.getUserAgent(request);

        ResponseCookie logoutCookie = authService.createLogoutCookie();
        response.addHeader("Set-Cookie", logoutCookie.toString());

        auditLogService.recordLogout(user, clientIp, userAgent);

        log.info("로그아웃 성공 - userId: {}, clientIp: {}", user.getId(), clientIp);
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
}
