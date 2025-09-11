package com.toktot.domain.user.service;

import com.toktot.interceptor.logging.SecurityEventLogger;
import com.toktot.domain.user.AuditLog;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityEventLogger securityEventLogger;

    public void recordLoginSuccess(User user, String clientIp, String userAgent, String loginMethod) {
        try {
            AuditLog auditLog = AuditLog.createLoginSuccess(user, clientIp, userAgent, loginMethod);
            auditLogRepository.save(auditLog);

            securityEventLogger.logLoginSuccess(
                    user.getId().toString(),
                    user.getEmail(),
                    clientIp,
                    userAgent,
                    loginMethod
            );

            log.atDebug()
                    .setMessage("Login success audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("method", loginMethod)
                    .addKeyValue("clientIp", clientIp)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to record login success audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("method", loginMethod)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        }
    }

    public void recordLoginFailure(String identifier, String clientIp, String userAgent, String reason) {
        try {
            AuditLog auditLog = AuditLog.createLoginFailed(identifier, clientIp, userAgent, reason);
            auditLogRepository.save(auditLog);

            securityEventLogger.logLoginFailure(identifier, clientIp, userAgent, reason);

            log.atDebug()
                    .setMessage("Login failure audit log recorded")
                    .addKeyValue("identifier", identifier)
                    .addKeyValue("reason", reason)
                    .addKeyValue("clientIp", clientIp)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to record login failure audit log")
                    .addKeyValue("identifier", identifier)
                    .addKeyValue("reason", reason)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        }
    }

    public void recordLogout(User user, String clientIp, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.createLogout(user, clientIp, userAgent);
            auditLogRepository.save(auditLog);

            securityEventLogger.logLogout(
                    user.getId().toString(),
                    clientIp,
                    userAgent
            );

            log.atDebug()
                    .setMessage("Logout audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("clientIp", clientIp)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to record logout audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        }
    }

    public void recordPasswordChange(User user, String clientIp, String userAgent) {
        try {
            securityEventLogger.logPasswordChange(
                    user.getId().toString(),
                    clientIp,
                    userAgent
            );

            log.atDebug()
                    .setMessage("Password change audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("clientIp", clientIp)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to record password change audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        }
    }

    public void recordAccountLocked(User user, String clientIp, String reason) {
        try {
            securityEventLogger.logAccountLocked(
                    user.getId().toString(),
                    clientIp,
                    reason
            );

            log.atWarn()
                    .setMessage("Account locked audit log recorded")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("reason", reason)
                    .addKeyValue("clientIp", clientIp)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to record account locked audit log")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("reason", reason)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        }
    }

    public void recordSuspiciousActivity(String identifier, String clientIp, String userAgent,
                                         String activity, String details) {
        try {
            securityEventLogger.logSuspiciousActivity(identifier, clientIp, userAgent, activity, details);

            log.atWarn()
                    .setMessage("Suspicious activity audit log recorded")
                    .addKeyValue("identifier", identifier)
                    .addKeyValue("activity", activity)
                    .addKeyValue("clientIp", clientIp)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to record suspicious activity audit log")
                    .addKeyValue("identifier", identifier)
                    .addKeyValue("activity", activity)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
        }
    }
}
