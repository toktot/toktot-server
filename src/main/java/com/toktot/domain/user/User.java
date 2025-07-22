package com.toktot.domain.user;

import com.toktot.domain.user.type.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_oauth_id", columnList = "oauth_id"),
        @Index(name = "idx_users_auth_provider", columnList = "auth_provider"),
        @Index(name = "idx_users_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 100, unique = true)
    private String oauthId;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider authProvider;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserAgreement userAgreement;

    public boolean isEnabled() {
        return userProfile == null || userProfile.canLogin();
    }

    public boolean isAccountNonLocked() {
        return userProfile == null || !userProfile.isAccountLocked();
    }

    public void assignUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            userProfile.assignUser(this);
        }
    }

    public void assignUserAgreement(UserAgreement userAgreement) {
        this.userAgreement = userAgreement;
        if (userAgreement != null) {
            userAgreement.assignUser(this);
        }
    }

    public void updatePassword(String encodedPassword) {
        if (StringUtils.hasText(encodedPassword)) {
            this.password = encodedPassword;
        }
    }
}
