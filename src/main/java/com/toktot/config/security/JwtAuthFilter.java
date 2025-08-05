package com.toktot.config.security;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final SecurityErrorResponseUtil securityErrorResponseUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        try {
            String jwt = extractTokenFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                if (jwtTokenProvider.isTokenExpired(jwt)) {
                    log.warn("JWT 토큰 만료 - uri: {}, token: {}...",
                            requestURI, jwt.substring(0, Math.min(20, jwt.length())));
                    securityErrorResponseUtil.writeErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
                    return;
                }

                if (jwtTokenProvider.validateToken(jwt) && jwtTokenProvider.isAccessToken(jwt)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                    setAuthenticationFromUserId(userId, request);
                    log.debug("JWT 인증 성공 - userId: {}, uri: {}", userId, requestURI);
                } else {
                    log.warn("JWT 토큰 무효 - uri: {}, token: {}...",
                            requestURI, jwt.substring(0, Math.min(20, jwt.length())));
                    securityErrorResponseUtil.writeErrorResponse(response, ErrorCode.TOKEN_INVALID);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생 - uri: {}, error: {}", requestURI, e.getMessage());
            securityErrorResponseUtil.writeErrorResponse(response, ErrorCode.TOKEN_INVALID);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private void setAuthenticationFromUserId(Long userId, HttpServletRequest request) {
        try {
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                log.warn("JWT 토큰의 사용자가 존재하지 않음 - userId: {}", userId);
                return;
            }

            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                log.warn("비활성화되거나 잠긴 계정 - userId: {}", userId);
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("사용자 인증 완료 - userId: {}, email: {}", user.getId(), user.getEmail());

        } catch (Exception e) {
            log.error("사용자 인증 설정 중 오류 - userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SecurityConfig.isPublicUrl(request.getRequestURI());
    }
}
