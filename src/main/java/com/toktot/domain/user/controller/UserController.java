package com.toktot.domain.user.controller;

import com.toktot.common.util.ClientInfoExtractor;
import com.toktot.domain.user.User;
import com.toktot.domain.user.service.AuditLogService;
import com.toktot.domain.user.service.AuthService;
import com.toktot.domain.user.service.UserDeleteService;
import com.toktot.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserDeleteService userDeleteService;
    private final AuditLogService auditLogService;

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal User user,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("회원 탈퇴 요청 - userId: {}, email: {}", user.getId(), user.getEmail());

        String clientIp = ClientInfoExtractor.getClientIp(request);
        String userAgent = ClientInfoExtractor.getUserAgent(request);

        auditLogService.recordUserDelete(user, clientIp, userAgent);
        userDeleteService.deleteUser(user.getId());
        ResponseCookie deleteRefreshToken = authService.deleteRefreshToken();
        response.addHeader("Set-Cookie", deleteRefreshToken.toString());

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }
}
