package com.toktot.domain.user.repository;

import com.toktot.domain.user.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
