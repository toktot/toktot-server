package com.toktot.common.logging;

import com.toktot.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String CLIENT_IP_KEY = "clientIp";
    private static final String USER_AGENT_KEY = "userAgent";
    private static final String REQUEST_URI_KEY = "requestUri";
    private static final String REQUEST_METHOD_KEY = "requestMethod";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString();

        // MDC에 추가하면 모든 로그에 자동으로 포함됨
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(CLIENT_IP_KEY, getClientIp(request));
        MDC.put(USER_AGENT_KEY, getUserAgent(request));
        MDC.put(REQUEST_URI_KEY, request.getRequestURI());
        MDC.put(REQUEST_METHOD_KEY, request.getMethod());

        String userId = getCurrentUserId();
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId);
        }

        log.atInfo()
                .setMessage("HTTP Request Started")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("uri", request.getRequestURI())
                .addKeyValue("queryString", request.getQueryString())
                .addKeyValue("userAgent", getUserAgent(request))
                .addKeyValue("contentType", request.getContentType())
                .addKeyValue("contentLength", request.getContentLength())
                .addKeyValue("remoteAddr", request.getRemoteAddr())
                .log();

        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            long startTime = (Long) request.getAttribute("startTime");
            long duration = System.currentTimeMillis() - startTime;

            if (ex == null) {
                log.atInfo()
                        .setMessage("HTTP Request Completed")
                        .addKeyValue("status", response.getStatus())
                        .addKeyValue("duration", duration)
                        .addKeyValue("contentType", response.getContentType())
                        .addKeyValue("responseSize", getResponseSize(response))
                        .log();
            } else {
                log.atError()
                        .setMessage("HTTP Request Failed")
                        .addKeyValue("status", response.getStatus())
                        .addKeyValue("duration", duration)
                        .addKeyValue("exception", ex.getClass().getSimpleName())
                        .addKeyValue("errorMessage", ex.getMessage())
                        .setCause(ex)
                        .log();
            }

            if (duration > 5000) {
                log.atWarn()
                        .setMessage("Slow Request Detected")
                        .addKeyValue("duration", duration)
                        .addKeyValue("uri", request.getRequestURI())
                        .addKeyValue("method", request.getMethod())
                        .log();
            }

        } catch (Exception e) {
            log.atError()
                    .setMessage("Error in request logging")
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : "0.0.0.0";
    }

    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && !userAgent.isEmpty()) ? userAgent : "Unknown";
    }

    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getId().toString();
            }
        } catch (Exception e) {
            // 인증 정보 조회 실패 시 무시
        }
        return null;
    }

    private long getResponseSize(HttpServletResponse response) {
        try {
            String contentLength = response.getHeader("Content-Length");
            if (contentLength != null) {
                return Long.parseLong(contentLength);
            }
        } catch (NumberFormatException e) {
            // 파싱 실패 시 무시
        }
        return 0;
    }
}
