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
import java.util.Date;

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
        log.atInfo()
                .setMessage("Token refresh request received")
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", userAgent.substring(0, Math.min(50, userAgent.length())))
                .log();
    }

    private String extractAndValidateRefreshToken(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  String clientIp) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (!StringUtils.hasText(refreshToken)) {
            log.atWarn()
                    .setMessage("Refresh token not found in cookie")
                    .addKeyValue("clientIp", clientIp)
                    .log();
            throw new ToktotException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 필요합니다.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.atWarn()
                    .setMessage("Invalid refresh token")
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("tokenLength", refreshToken.length())
                    .log();
            clearRefreshTokenCookie(response);
            throw new ToktotException(ErrorCode.TOKEN_INVALID, "리프레시 토큰이 유효하지 않습니다.");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.atWarn()
                    .setMessage("Token is not refresh type")
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("tokenType", jwtTokenProvider.getTokenType(refreshToken))
                    .log();
            clearRefreshTokenCookie(response);
            throw new ToktotException(ErrorCode.TOKEN_INVALID, "올바른 리프레시 토큰이 아닙니다.");
        }

        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            log.atWarn()
                    .setMessage("Refresh token expired")
                    .addKeyValue("clientIp", clientIp)
                    .addKeyValue("expiration", jwtTokenProvider.getExpirationFromToken(refreshToken))
                    .log();
            clearRefreshTokenCookie(response);
            throw new ToktotException(ErrorCode.TOKEN_EXPIRED, "리프레시 토큰이 만료되었습니다.");
        }

        log.atDebug()
                .setMessage("Refresh token validation successful")
                .addKeyValue("clientIp", clientIp)
                .log();

        return refreshToken;
    }

    private User validateUserFromToken(String refreshToken, HttpServletResponse response, String clientIp) {
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.atWarn()
                            .setMessage("User not found for token refresh")
                            .addKeyValue("userId", userId)
                            .addKeyValue("clientIp", clientIp)
                            .log();
                    return new ToktotException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            log.atWarn()
                    .setMessage("Account disabled or locked during token refresh")
                    .addKeyValue("userId", userId)
                    .addKeyValue("isEnabled", user.isEnabled())
                    .addKeyValue("isAccountNonLocked", user.isAccountNonLocked())
                    .addKeyValue("clientIp", clientIp)
                    .log();
            clearRefreshTokenCookie(response);
            throw new ToktotException(ErrorCode.PERMISSION_DENIED, "계정이 비활성화되었거나 잠금 상태입니다.");
        }

        log.atDebug()
                .setMessage("User validation successful for token refresh")
                .addKeyValue("userId", userId)
                .addKeyValue("clientIp", clientIp)
                .log();

        return user;
    }

    private TokenResponse generateAndSetNewTokens(User user, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.generateTokens(user);

        ResponseCookie newRefreshCookie = authService.createRefreshTokenCookie(
                tokenResponse.refreshToken()
        );
        response.addHeader("Set-Cookie", newRefreshCookie.toString());

        log.atDebug()
                .setMessage("New tokens generated and cookie set")
                .addKeyValue("userId", user.getId())
                .addKeyValue("expiresIn", tokenResponse.expiresIn())
                .log();

        return tokenResponse;
    }

    private void updateUserLoginInfo(User user, String clientIp) {
        if (user.getUserProfile() != null) {
            user.getUserProfile().recordSuccessfulLogin(clientIp);
            userRepository.save(user);

            log.atDebug()
                    .setMessage("User login info updated during token refresh")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("clientIp", clientIp)
                    .log();
        }
    }

    private void logSuccessfulRefresh(User user, String clientIp) {
        log.atInfo()
                .setMessage("Token refresh successful")
                .addKeyValue("userId", user.getId())
                .addKeyValue("authProvider", user.getAuthProvider())
                .addKeyValue("clientIp", clientIp)
                .log();
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

        log.atDebug()
                .setMessage("Refresh token cookie cleared")
                .log();
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleRefreshBusinessError(
            ToktotException e, String clientIp) {

        log.atWarn()
                .setMessage("Token refresh business failure")
                .addKeyValue("errorCode", e.getErrorCodeName())
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("clientIp", clientIp)
                .log();

        return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> handleRefreshSystemError(
            Exception e, String clientIp) {

        log.atError()
                .setMessage("Token refresh system error")
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("error", e.getMessage())
                .setCause(e)
                .log();

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
