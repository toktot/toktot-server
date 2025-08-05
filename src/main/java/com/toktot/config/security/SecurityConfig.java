package com.toktot.config.security;

import com.toktot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityErrorResponseUtil errorResponseUtil;
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
            "/v1/auth/**",
            "/actuator/health",
            "/actuator/info"
    };

    private static final String[] PROTECTED_URLS = {
            "/v1/users/**",
            "/v1/reviews/**",
            "/v1/folders/**",
            "/v1/routes/**"
    };

    private static final List<String> DEFAULT_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    );

    private static final List<String> DEFAULT_HEADERS = Arrays.asList("*");

    private static final List<String> EXPOSED_HEADERS = Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
    );

    public static boolean isPublicUrl(String path) {
        return Arrays.stream(PUBLIC_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Spring Security 설정 초기화 시작");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(PROTECTED_URLS).authenticated()
                        .anyRequest().authenticated())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(createAuthenticationEntryPoint())
                        .accessDeniedHandler(createAccessDeniedHandler()))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("CORS 설정 구성 - Origins: {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configureCorsOrigins(configuration);
        configureCorsMethodsAndHeaders(configuration);
        configureAdvancedCors(configuration);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.debug("CORS 설정 완료");
        return source;
    }

    private AuthenticationEntryPoint createAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            logSecurityEvent("인증 실패", request, authException.getMessage());
            handleSecurityError(response, ErrorCode.LOGIN_REQUIRED, "Authentication error");
        };
    }

    private AccessDeniedHandler createAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            logSecurityEvent("권한 거부", request, accessDeniedException.getMessage());
            handleSecurityError(response, ErrorCode.ACCESS_DENIED, "Access denied error");
        };
    }

    private void logSecurityEvent(String eventType, HttpServletRequest request, String errorMessage) {
        log.warn("{} - URI: {}, IP: {}, Error: {}",
                eventType,
                request.getRequestURI(),
                getClientIp(request),
                errorMessage);
    }

    private void handleSecurityError(jakarta.servlet.http.HttpServletResponse response,
                                     ErrorCode errorCode, String context) {
        try {
            errorResponseUtil.writeErrorResponse(response, errorCode);
        } catch (Exception e) {
            log.error("{} response 작성 중 오류: {}", context, e.getMessage(), e);
            response.setStatus(500);
        }
    }

    private void configureCorsOrigins(CorsConfiguration configuration) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return;
        }

        List<String> directOrigins = allowedOrigins.stream()
                .filter(origin -> !origin.contains("*"))
                .collect(Collectors.toList());

        List<String> patternOrigins = allowedOrigins.stream()
                .filter(origin -> origin.contains("*"))
                .collect(Collectors.toList());

        if (!directOrigins.isEmpty()) {
            configuration.setAllowedOrigins(directOrigins);
        }
        if (!patternOrigins.isEmpty()) {
            configuration.setAllowedOriginPatterns(patternOrigins);
        }
    }

    private void configureCorsMethodsAndHeaders(CorsConfiguration configuration) {
        configuration.setAllowedMethods(
                allowedMethods != null ? allowedMethods : DEFAULT_METHODS
        );
        configuration.setAllowedHeaders(
                allowedHeaders != null ? allowedHeaders : DEFAULT_HEADERS
        );
    }

    private void configureAdvancedCors(CorsConfiguration configuration) {
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
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
