package com.toktot.domain.user;

import com.toktot.domain.user.type.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_time", columnList = "user_id, created_at"),
        @Index(name = "idx_audit_action_time", columnList = "action, created_at"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(
            foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL"
    ))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private AuditAction action;

    @Column(length = 100)
    private String resource;

    @Column(length = 45, nullable = false)
    private String clientIp;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuditLog createUserAction(User user, AuditAction action, String resource,
                                            String clientIp, String userAgent, String metadata) {
        validateRequiredFields(action, clientIp, userAgent);

        return AuditLog.builder()
                .user(user)
                .action(action)
                .resource(resource)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .metadata(metadata)
                .build();
    }

    public static AuditLog createAnonymousAction(AuditAction action, String resource,
                                                 String clientIp, String userAgent, String metadata) {
        validateRequiredFields(action, clientIp, userAgent);

        return AuditLog.builder()
                .action(action)
                .resource(resource)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .metadata(metadata)
                .build();
    }

    public static AuditLog createLoginSuccess(User user, String clientIp, String userAgent, String loginMethod) {
        String metadata = String.format("{\"login_method\": \"%s\", \"timestamp\": \"%s\"}",
                loginMethod, LocalDateTime.now());
        return createUserAction(user, AuditAction.LOGIN_SUCCESS, "/api/auth/login", clientIp, userAgent, metadata);
    }

    public static AuditLog createLoginFailed(String email, String clientIp, String userAgent, String reason) {
        String metadata = String.format("{\"email\": \"%s\", \"reason\": \"%s\", \"timestamp\": \"%s\"}",
                email, reason, LocalDateTime.now());
        return createAnonymousAction(AuditAction.LOGIN_FAILED, "/api/auth/login", clientIp, userAgent, metadata);
    }

    public static AuditLog createLogout(User user, String clientIp, String userAgent) {
        String metadata = String.format("{\"logout_time\": \"%s\", \"client_ip\": \"%s\"}",
                LocalDateTime.now(), clientIp);
        return createUserAction(user, AuditAction.LOGOUT, "/v1/logout", clientIp, userAgent, metadata);
    }

    private static void validateRequiredFields(AuditAction action, String clientIp, String userAgent) {
        if (action == null) {
            throw new IllegalArgumentException("액션은 필수입니다.");
        }
        if (!StringUtils.hasText(clientIp)) {
            throw new IllegalArgumentException("클라이언트 IP는 필수입니다.");
        }
        if (!StringUtils.hasText(userAgent)) {
            throw new IllegalArgumentException("User Agent는 필수입니다.");
        }
    }
}
