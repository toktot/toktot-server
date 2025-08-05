package com.toktot.domain.user.service;

import com.toktot.config.security.JwtTokenProvider;
import com.toktot.domain.user.User;
import com.toktot.web.dto.auth.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    public TokenResponse generateTokens(User user) {
        log.debug("JWT 토큰 생성 시작 - userId: {}, email: {}", user.getId(), user.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        long expiresIn = jwtTokenProvider.getExpirationTimeInSeconds(accessToken);

        log.info("JWT 토큰 생성 완료 - userId: {}, expiresIn: {}초, authProvider: {}",
                user.getId(), expiresIn, user.getAuthProvider());

        return TokenResponse.of(accessToken, refreshToken, expiresIn);
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        log.debug("리프레시 토큰 쿠키 생성 - maxAge: {}일",
                Duration.ofMillis(refreshTokenExpirationMs).toDays());

        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .path("/")
                .build();
    }

    public ResponseCookie createLogoutCookie() {
        log.debug("로그아웃 쿠키 생성 (리프레시 토큰 제거)");

        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(Duration.ZERO)
                .path("/")
                .build();
    }
}
