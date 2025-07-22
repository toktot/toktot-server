package com.toktot.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_agreements", indexes = {
        @Index(name = "idx_agreements_terms", columnList = "terms_agreed, terms_agreed_at"),
        @Index(name = "idx_agreements_privacy", columnList = "privacy_agreed, privacy_agreed_at"),
        @Index(name = "idx_agreements_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserAgreement {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn
    private User user;

    @Column(nullable = false)
    private Boolean termsAgreed;

    private LocalDateTime termsAgreedAt;

    private LocalDateTime termsWithdrawnAt;

    @Column(nullable = false)
    private Boolean privacyAgreed;

    private LocalDateTime privacyAgreedAt;

    private LocalDateTime privacyWithdrawnAt;

    @Column(length = 45, nullable = false)
    private String clientIp;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userAgent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserAgreement createWithFullAgreement(User user, String clientIp, String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        return UserAgreement.builder()
                .user(user)
                .termsAgreed(true)
                .termsAgreedAt(now)
                .privacyAgreed(true)
                .privacyAgreedAt(now)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
    }

    void assignUser(User user) {
        this.user = user;
    }
}
