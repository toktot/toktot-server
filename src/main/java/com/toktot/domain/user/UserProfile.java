package com.toktot.domain.user;

import com.toktot.domain.user.type.AccountStatus;
import com.toktot.domain.user.type.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserProfile {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn
    private User user;

    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(nullable = false, columnDefinition = "SMALLINT")
    @Builder.Default
    private Byte failedLoginCount = 0;

    private LocalDateTime lockedUntil;

    private LocalDateTime lastLoginAt;

    @Column(length = 45)
    private String lastLoginIp;

    private LocalDateTime passwordChangedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    void assignUser(User user) {
        this.user = user;
    }

    public void recordSuccessfulLogin(String clientIp) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = clientIp;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isAccountActive() {
        return AccountStatus.ACTIVE.equals(this.accountStatus);
    }

    public boolean canLogin() {
        return isAccountActive() && !isAccountLocked();
    }

    public static UserProfile createDefault(User user) {
        return UserProfile.builder()
                .user(user)
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginCount((byte) 0)
                .build();
    }
}
