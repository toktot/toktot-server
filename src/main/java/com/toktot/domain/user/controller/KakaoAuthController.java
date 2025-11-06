package com.toktot.domain.user.controller;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.service.AuthService;
import com.toktot.domain.user.service.KakaoOAuth2Service;
import com.toktot.domain.user.service.AuditLogService;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.user.dto.request.login.KakaoLoginRequest;
import com.toktot.domain.user.dto.response.TokenResponse;
import com.toktot.domain.user.dto.mapper.AuthResponseMapper;
import com.toktot.common.util.ClientInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoOAuth2Service kakaoOAuth2Service;
    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final AuthResponseMapper authResponseMapper;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);
        String userAgent = ClientInfoExtractor.getUserAgent(httpRequest);

        logLoginAttempt(clientIp, userAgent, request.code());

        try {
            User user = processKakaoAuthentication(request, clientIp, userAgent);
            TokenResponse tokenResponse = generateAndSetTokens(user, httpResponse);
            recordSuccessfulLogin(user, clientIp, userAgent);

            logSuccessfulLogin(user, clientIp);

            String successMessage = authResponseMapper.toLoginSuccessMessage(user);
            TokenResponse publicTokenResponse = authResponseMapper.toPublicTokenResponse(tokenResponse);

            return ResponseEntity.ok(
                    ApiResponse.success(successMessage, publicTokenResponse)
            );

        } catch (ToktotException e) {
            return handleLoginBusinessError(e, clientIp, userAgent);
        } catch (Exception e) {
            return handleLoginSystemError(e, clientIp, userAgent);
        }
    }

    private void logLoginAttempt(String clientIp, String userAgent, String authCode) {
        log.atInfo()
                .setMessage("Kakao login API request received")
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", userAgent.substring(0, Math.min(50, userAgent.length())))
                .addKeyValue("authCodeLength", authCode.length())
                .addKeyValue("authCodePrefix", authCode.substring(0, Math.min(8, authCode.length())))
                .log();
    }

    private User processKakaoAuthentication(KakaoLoginRequest request, String clientIp, String userAgent) {
        log.atDebug()
                .setMessage("Processing Kakao authentication")
                .addKeyValue("clientIp", clientIp)
                .log();

        return kakaoOAuth2Service.processKakaoLogin(request.code(), clientIp, userAgent);
    }

    private TokenResponse generateAndSetTokens(User user, HttpServletResponse httpResponse) {
        TokenResponse tokenResponse = authService.generateTokens(user);

        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(
                tokenResponse.refreshToken()
        );
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        log.atDebug()
                .setMessage("JWT tokens generated and cookie set for Kakao login")
                .addKeyValue("userId", user.getId())
                .log();

        return tokenResponse;
    }

    private void recordSuccessfulLogin(User user, String clientIp, String userAgent) {
        try {
            auditLogService.recordLoginSuccess(user, clientIp, userAgent, "KAKAO");

            log.atDebug()
                    .setMessage("Kakao login audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .log();
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("Failed to record Kakao login audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("error", e.getMessage())
                    .log();
        }
    }

    private void logSuccessfulLogin(User user, String clientIp) {
        log.atInfo()
                .setMessage("Kakao login API successful")
                .addKeyValue("userId", user.getId())
                .addKeyValue("nickname", user.getNickname())
                .addKeyValue("authProvider", user.getAuthProvider())
                .addKeyValue("clientIp", clientIp)
                .log();
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleLoginBusinessError(
            ToktotException e, String clientIp, String userAgent) {

        log.atWarn()
                .setMessage("Kakao login API business failure")
                .addKeyValue("errorCode", e.getErrorCodeName())
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("clientIp", clientIp)
                .log();

        auditLogService.recordLoginFailure("kakao_user", clientIp, userAgent, e.getMessage());

        return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleLoginSystemError(
            Exception e, String clientIp, String userAgent) {

        log.atError()
                .setMessage("Kakao login API system error")
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", userAgent.substring(0, Math.min(50, userAgent.length())))
                .addKeyValue("error", e.getMessage())
                .setCause(e)
                .log();

        auditLogService.recordLoginFailure("kakao_user", clientIp, userAgent, "시스템 오류");

        return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }
}
