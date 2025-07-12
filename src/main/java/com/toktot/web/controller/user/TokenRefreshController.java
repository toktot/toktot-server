package com.toktot.web.controller.user;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.config.security.JwtTokenProvider;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.domain.user.service.AuthService;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.auth.response.TokenResponse;
import com.toktot.web.util.ClientInfoExtractor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenRefreshController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String clientIp = ClientInfoExtractor.getClientIp(request);
        String userAgent = ClientInfoExtractor.getUserAgent(request);

        logRefreshAttempt(clientIp, userAgent);

        try {
            String refreshToken = extractAndValidateRefreshToken(request, response, clientIp);
            User user = validateUserFromToken(refreshToken, response, clientIp);
            TokenResponse tokenResponse = generateAndSetNewTokens(user, response);
            updateUserLoginInfo(user, clientIp);

            logSuccessfulRefresh(user, clientIp);

            return ResponseEntity.ok(
                    ApiResponse.success("토큰이 갱신되었습니다.",
                            createPublicTokenResponse(tokenResponse))
            );

        } catch (ToktotException e) {
            return handleRefreshBusinessError(e, clientIp);
        } catch (Exception e) {
            return handleRefreshSystemError(e, clientIp);
        }
    }

    private void logRefreshAttempt(String clientIp, String userAgent) {
        log.info("토큰 갱신 요청 - clientIp: {}, userAgent: {}",
                clientIp, userAgent.substring(0, Math.min(50, userAgent.length())));
    }

    private String extractAndValidateRefreshToken(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  String clientIp) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (!StringUtils.hasText(refreshToken)) {
            log.warn("리프레시 토큰이 없음 - clientIp: {}", clientIp);
            throw new ToktotException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 필요합니다.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 리프레시 토큰 - clientIp: {}", clientIp);
            clearRefreshTokenCookie(response);
            throw new ToktotException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 유효하지 않습니다.");
        }

        return refreshToken;
    }

    private User validateUserFromToken(String refreshToken, HttpServletResponse response, String clientIp) {
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 - userId: {}, clientIp: {}", userId, clientIp);
                    return new ToktotException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            log.warn("비활성화되거나 잠긴 계정 - userId: {}, clientIp: {}", userId, clientIp);
            clearRefreshTokenCookie(response);
            throw new ToktotException(ErrorCode.PERMISSION_DENIED, "계정이 비활성화되었거나 잠금 상태입니다.");
        }

        return user;
    }

    private TokenResponse generateAndSetNewTokens(User user, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.generateTokens(user);

        ResponseCookie newRefreshCookie = authService.createRefreshTokenCookie(
                tokenResponse.refreshToken()
        );
        response.addHeader("Set-Cookie", newRefreshCookie.toString());

        return tokenResponse;
    }

    private void updateUserLoginInfo(User user, String clientIp) {
        if (user.getUserProfile() != null) {
            user.getUserProfile().recordSuccessfulLogin(clientIp);
            userRepository.save(user);
        }
    }

    private void logSuccessfulRefresh(User user, String clientIp) {
        log.info("토큰 갱신 성공 - userId: {}, authProvider: {}, clientIp: {}",
                user.getId(), user.getAuthProvider(), clientIp);
    }

    private TokenResponse createPublicTokenResponse(TokenResponse tokenResponse) {
        return TokenResponse.of(
                tokenResponse.accessToken(),
                null,
                tokenResponse.expiresIn()
        );
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie clearCookie = authService.createLogoutCookie();
        response.addHeader("Set-Cookie", clearCookie.toString());
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleRefreshBusinessError(
            ToktotException e, String clientIp) {

        log.warn("토큰 갱신 비즈니스 실패 - errorCode: {}, message: {}, clientIp: {}",
                e.getErrorCodeName(), e.getMessage(), clientIp);

        return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleRefreshSystemError(
            Exception e, String clientIp) {

        log.error("토큰 갱신 중 시스템 오류 - clientIp: {}, error: {}", clientIp, e.getMessage(), e);
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
