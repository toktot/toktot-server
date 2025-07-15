package com.toktot.config.security;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${toktot.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${toktot.security.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${toktot.security.cors.allowed-headers}")
    private List<String> allowedHeaders;

    @Value("${toktot.security.cors.max-age}")
    private long maxAge;

    @Value("${toktot.security.cors.allow-credentials}")
    private boolean allowCredentials;

    private static final String[] PUBLIC_URLS = {
            // 인증 관련 API
            "/api/v1/auth/**",

            // OAuth2 관련
            "/oauth2/**",
            "/login/oauth2/**",

            // 헬스체크 및 모니터링
            "/api/health",
            "/actuator/health",
            "/actuator/info",
    };

    public static boolean isPublicUrl(String path) {
        return Arrays.stream(PUBLIC_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    static final String[] PROTECTED_URLS = {
            "/api/v1/users/**",
            "/api/v1/reviews/**",
            "/api/v1/bookmarks/**",
            "/api/v1/routes/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Spring Security 설정 초기화 시작 - JWT 인증 적용");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/api/v1/auth/kakao/login")
                        .defaultSuccessUrl("/api/v1/auth/kakao/callback")
                        .failureUrl("/api/v1/auth/kakao/failure")
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(PROTECTED_URLS).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("인증되지 않은 요청 - URI: {}, IP: {}, Error: {}",
                                    request.getRequestURI(),
                                    getClientIp(request),
                                    authException.getMessage());
                            throw new ToktotException(ErrorCode.LOGIN_REQUIRED);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("접근 권한 없음 - URI: {}, IP: {}, Error: {}",
                                    request.getRequestURI(),
                                    getClientIp(request),
                                    accessDeniedException.getMessage());
                            throw new ToktotException(ErrorCode.ACCESS_DENIED);
                        })
                );

        log.info("Spring Security 설정 완료 - JWT 필터 적용됨");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("CORS 설정 구성 시작 - Origins: {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.debug("CORS 설정 완료");
        return source;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
