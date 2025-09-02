package com.toktot.interceptor.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityEventLogger {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");

    public void logLoginSuccess(String userId, String email, String clientIp, String userAgent, String loginMethod) {
        SECURITY_LOG.atInfo()
                .setMessage("User login successful")
                .addKeyValue("event", "LOGIN_SUCCESS")
                .addKeyValue("userId", userId)
                .addKeyValue("email", email)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", truncateUserAgent(userAgent))
                .addKeyValue("loginMethod", loginMethod)
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.debug("보안 이벤트 로그 기록 완료 - 로그인 성공: userId={}, method={}", userId, loginMethod);
    }

    public void logLoginFailure(String identifier, String clientIp, String userAgent, String reason) {
        SECURITY_LOG.atWarn()
                .setMessage("User login failed")
                .addKeyValue("event", "LOGIN_FAILURE")
                .addKeyValue("identifier", identifier)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", truncateUserAgent(userAgent))
                .addKeyValue("reason", reason)
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.debug("보안 이벤트 로그 기록 완료 - 로그인 실패: identifier={}, reason={}", identifier, reason);
    }

    public void logLogout(String userId, String clientIp, String userAgent) {
        SECURITY_LOG.atInfo()
                .setMessage("User logout")
                .addKeyValue("event", "LOGOUT")
                .addKeyValue("userId", userId)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", truncateUserAgent(userAgent))
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.debug("보안 이벤트 로그 기록 완료 - 로그아웃: userId={}", userId);
    }

    public void logPasswordChange(String userId, String clientIp, String userAgent) {
        SECURITY_LOG.atInfo()
                .setMessage("Password changed")
                .addKeyValue("event", "PASSWORD_CHANGE")
                .addKeyValue("userId", userId)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", truncateUserAgent(userAgent))
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.debug("보안 이벤트 로그 기록 완료 - 비밀번호 변경: userId={}", userId);
    }

    public void logAccountLocked(String userId, String clientIp, String reason) {
        SECURITY_LOG.atWarn()
                .setMessage("Account locked")
                .addKeyValue("event", "ACCOUNT_LOCKED")
                .addKeyValue("userId", userId)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("reason", reason)
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.warn("보안 이벤트 로그 기록 완료 - 계정 잠금: userId={}, reason={}", userId, reason);
    }

    public void logSuspiciousActivity(String identifier, String clientIp, String userAgent, String activity, String details) {
        SECURITY_LOG.atWarn()
                .setMessage("Suspicious activity detected")
                .addKeyValue("event", "SUSPICIOUS_ACTIVITY")
                .addKeyValue("identifier", identifier)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", truncateUserAgent(userAgent))
                .addKeyValue("activity", activity)
                .addKeyValue("details", details)
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.warn("보안 이벤트 로그 기록 완료 - 의심스러운 활동: identifier={}, activity={}", identifier, activity);
    }

    public void logTokenExpired(String userId, String tokenType, String clientIp) {
        SECURITY_LOG.atInfo()
                .setMessage("Token expired")
                .addKeyValue("event", "TOKEN_EXPIRED")
                .addKeyValue("userId", userId)
                .addKeyValue("tokenType", tokenType)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.debug("보안 이벤트 로그 기록 완료 - 토큰 만료: userId={}, tokenType={}", userId, tokenType);
    }

    public void logUnauthorizedAccess(String requestUri, String clientIp, String userAgent, String reason) {
        SECURITY_LOG.atWarn()
                .setMessage("Unauthorized access attempt")
                .addKeyValue("event", "UNAUTHORIZED_ACCESS")
                .addKeyValue("requestUri", requestUri)
                .addKeyValue("clientIp", clientIp)
                .addKeyValue("userAgent", truncateUserAgent(userAgent))
                .addKeyValue("reason", reason)
                .addKeyValue("timestamp", System.currentTimeMillis())
                .log();

        log.warn("보안 이벤트 로그 기록 완료 - 무단 접근 시도: uri={}, clientIp={}", requestUri, clientIp);
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        return userAgent.length() > 200 ? userAgent.substring(0, 200) + "..." : userAgent;
    }
}
