package com.toktot.domain.user.service;

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

    public void recordLoginSuccess(User user, String clientIp, String userAgent, String loginMethod) {
        try {
            AuditLog auditLog = AuditLog.createLoginSuccess(user, clientIp, userAgent, loginMethod);
            auditLogRepository.save(auditLog);

            log.debug("로그인 성공 감사로그 기록 완료 - userId: {}, method: {}",
                    user.getId(), loginMethod);
        } catch (Exception e) {
            log.error("로그인 성공 감사로그 기록 실패 - userId: {}, error: {}",
                    user.getId(), e.getMessage(), e);
        }
    }

    public void recordLoginFailure(String identifier, String clientIp, String userAgent, String reason) {
        try {
            AuditLog auditLog = AuditLog.createLoginFailed(identifier, clientIp, userAgent, reason);
            auditLogRepository.save(auditLog);

            log.debug("로그인 실패 감사로그 기록 완료 - identifier: {}, reason: {}",
                    identifier, reason);
        } catch (Exception e) {
            log.error("로그인 실패 감사로그 기록 실패 - identifier: {}, error: {}",
                    identifier, e.getMessage(), e);
        }
    }

}
