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

    public void agreeToTerms(String clientIp, String userAgent) {
        this.termsAgreed = true;
        this.termsAgreedAt = LocalDateTime.now();
        this.termsWithdrawnAt = null;
        updateAgreementInfo(clientIp, userAgent);
    }

    public void withdrawTerms(String clientIp, String userAgent) {
        this.termsAgreed = false;
        this.termsWithdrawnAt = LocalDateTime.now();
        updateAgreementInfo(clientIp, userAgent);
    }

    public void agreeToPrivacy(String clientIp, String userAgent) {
        this.privacyAgreed = true;
        this.privacyAgreedAt = LocalDateTime.now();
        this.privacyWithdrawnAt = null;
        updateAgreementInfo(clientIp, userAgent);
    }

    public void withdrawPrivacy(String clientIp, String userAgent) {
        this.privacyAgreed = false;
        this.privacyWithdrawnAt = LocalDateTime.now();
        updateAgreementInfo(clientIp, userAgent);
    }

    public void agreeToAll(String clientIp, String userAgent) {
        agreeToTerms(clientIp, userAgent);
        agreeToPrivacy(clientIp, userAgent);
    }

    private void updateAgreementInfo(String clientIp, String userAgent) {
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    public boolean hasAgreedToAll() {
        return Boolean.TRUE.equals(termsAgreed) && Boolean.TRUE.equals(privacyAgreed);
    }

    public boolean hasWithdrawnAny() {
        return Boolean.FALSE.equals(termsAgreed) || Boolean.FALSE.equals(privacyAgreed);
    }

    public Long getUserId() {
        return this.id;
    }

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
}
