package com.toktot.web.controller.user;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.service.AuthService;
import com.toktot.domain.user.service.KakaoOAuth2Service;
import com.toktot.domain.user.service.AuditLogService;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.auth.request.login.KakaoLoginRequest;
import com.toktot.web.dto.auth.response.TokenResponse;
import com.toktot.web.util.ClientInfoExtractor;
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
@RequestMapping("/api/v1/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoOAuth2Service kakaoOAuth2Service;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(httpRequest);
        String userAgent = ClientInfoExtractor.getUserAgent(httpRequest);

        logLoginAttempt(clientIp, userAgent);

        try {
            User user = processKakaoAuthentication(request, clientIp, userAgent);
            TokenResponse tokenResponse = generateAndSetTokens(user, httpResponse);
            recordSuccessfulLogin(user, clientIp, userAgent);

            logSuccessfulLogin(user, clientIp);

            return ResponseEntity.ok(
                    ApiResponse.success("카카오 로그인이 완료되었습니다.",
                            createPublicTokenResponse(tokenResponse))
            );

        } catch (ToktotException e) {
            return handleLoginBusinessError(e, clientIp, userAgent);
        } catch (Exception e) {
            return handleLoginSystemError(e, clientIp, userAgent);
        }
    }

    private void logLoginAttempt(String clientIp, String userAgent) {
        log.info("카카오 로그인 API 호출 - clientIp: {}, userAgent: {}",
                clientIp, userAgent.substring(0, Math.min(50, userAgent.length())));
    }

    private User processKakaoAuthentication(KakaoLoginRequest request, String clientIp, String userAgent) {
        return kakaoOAuth2Service.processKakaoLogin(request.code(), clientIp, userAgent);
    }

    private TokenResponse generateAndSetTokens(User user, HttpServletResponse httpResponse) {
        TokenResponse tokenResponse = authService.generateTokens(user);

        ResponseCookie refreshCookie = authService.createRefreshTokenCookie(
                tokenResponse.refreshToken()
        );
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        return tokenResponse;
    }

    private void recordSuccessfulLogin(User user, String clientIp, String userAgent) {
        auditLogService.recordLoginSuccess(user, clientIp, userAgent, "KAKAO");
    }

    private void logSuccessfulLogin(User user, String clientIp) {
        log.info("카카오 로그인 API 성공 - userId: {}, nickname: {}, clientIp: {}",
                user.getId(), user.getNickname(), clientIp);
    }

    private TokenResponse createPublicTokenResponse(TokenResponse tokenResponse) {
        return TokenResponse.of(
                tokenResponse.accessToken(),
                null,
                tokenResponse.expiresIn()
        );
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleLoginBusinessError(
            ToktotException e, String clientIp, String userAgent) {

        log.warn("카카오 로그인 API 비즈니스 실패 - errorCode: {}, message: {}, clientIp: {}",
                e.getErrorCodeName(), e.getMessage(), clientIp);

        auditLogService.recordLoginFailure("kakao_user", clientIp, userAgent, e.getMessage());

        return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleLoginSystemError(
            Exception e, String clientIp, String userAgent) {

        log.error("카카오 로그인 API 시스템 오류 - clientIp: {}, userAgent: {}, error: {}",
                clientIp, userAgent, e.getMessage(), e);

        auditLogService.recordLoginFailure("kakao_user", clientIp, userAgent, "시스템 오류");

        return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }
}
