package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.UserAgreement;
import com.toktot.domain.user.UserProfile;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.domain.user.type.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    public void checkEmailDuplicate(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("이메일 중복 확인 실패 - email: {}", email);
            throw new ToktotException(ErrorCode.DUPLICATE_EMAIL);
        }

        log.debug("이메일 중복 확인 통과 - email: {}", email);
    }

    public void checkNicknameDuplicate(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            log.warn("닉네임 중복 확인 실패 - nickname: {}", nickname);
            throw new ToktotException(ErrorCode.DUPLICATE_USERNAME);
        }

        log.debug("닉네임 중복 확인 통과 - nickname: {}", nickname);
    }

    public User registerEmailUser(String email, String password, String nickname,
                                  String clientIp, String userAgent) {

        validateRegistrationPreconditions(email, nickname);

        User newUser = createEmailUser(email, password, nickname);
        User savedUser = userRepository.save(newUser);

        setupUserRelations(savedUser, clientIp, userAgent);
        User finalUser = userRepository.save(savedUser);

        emailVerificationService.clearVerificationStatus(email);

        sendWelcomeEmail(email, nickname);

        log.info("이메일 회원가입 완료 - userId: {}, email: {}, nickname: {}, clientIp: {}",
                finalUser.getId(), email, nickname, clientIp);

        return finalUser;
    }

    public User authenticateEmailUser(String email, String password, String clientIp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("이메일 로그인 실패 - 존재하지 않는 이메일: {}, clientIp: {}", email, clientIp);
                    return new ToktotException(ErrorCode.USER_NOT_FOUND);
                });

        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            log.warn("이메일 로그인 실패 - 이메일 계정이 아님: {}, provider: {}, clientIp: {}",
                    email, user.getAuthProvider(), clientIp);
            throw new ToktotException(ErrorCode.INVALID_PASSWORD);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("이메일 로그인 실패 - 비밀번호 불일치: {}, clientIp: {}", email, clientIp);
            handleFailedLogin(user, clientIp);
            throw new ToktotException(ErrorCode.INVALID_PASSWORD);
        }

        validateUserAccount(user, email, clientIp);

        handleSuccessfulLogin(user, clientIp);

        log.info("이메일 로그인 성공 - userId: {}, email: {}, clientIp: {}",
                user.getId(), email, clientIp);

        return user;
    }

    private void validateRegistrationPreconditions(String email, String nickname) {
        checkEmailDuplicate(email);
        checkNicknameDuplicate(nickname);

        if (!emailVerificationService.isVerified(email)) {
            log.warn("회원가입 실패 - 이메일 인증 미완료: {}", email);
            throw new ToktotException(ErrorCode.EMAIL_NOT_VERIFIED, "이메일 인증을 완료해주세요.");
        }
    }

    private User createEmailUser(String email, String password, String nickname) {
        String encodedPassword = passwordEncoder.encode(password);

        User newUser = User.builder()
                .email(email)
                .password(encodedPassword)
                .authProvider(AuthProvider.EMAIL)
                .nickname(nickname)
                .build();

        log.debug("이메일 사용자 객체 생성 완료 - email: {}, nickname: {}", email, nickname);
        return newUser;
    }

    private void setupUserRelations(User user, String clientIp, String userAgent) {
        UserProfile userProfile = UserProfile.createDefault(user);
        UserAgreement userAgreement = UserAgreement.createWithFullAgreement(user, clientIp, userAgent);

        user.assignUserProfile(userProfile);
        user.assignUserAgreement(userAgreement);

        log.debug("사용자 연관관계 설정 완료 - userId: {}", user.getId());
    }

    private void validateUserAccount(User user, String email, String clientIp) {
        if (!user.isEnabled()) {
            log.warn("이메일 로그인 실패 - 비활성화된 계정: {}, clientIp: {}", email, clientIp);
            throw new ToktotException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!user.isAccountNonLocked()) {
            log.warn("이메일 로그인 실패 - 잠긴 계정: {}, clientIp: {}", email, clientIp);
            throw new ToktotException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    private void handleSuccessfulLogin(User user, String clientIp) {
        if (user.getUserProfile() != null) {
            user.getUserProfile().recordSuccessfulLogin(clientIp);
            userRepository.save(user);
        }

        log.debug("로그인 성공 처리 완료 - userId: {}, clientIp: {}", user.getId(), clientIp);
    }

    private void handleFailedLogin(User user, String clientIp) {
        if (user.getUserProfile() != null) {
            user.getUserProfile().incrementFailedLoginCount();
            userRepository.save(user);

            log.debug("로그인 실패 처리 완료 - userId: {}, failedCount: {}, clientIp: {}",
                    user.getId(), user.getUserProfile().getFailedLoginCount(), clientIp);
        }
    }

    private void sendWelcomeEmail(String email, String nickname) {
        try {
            emailService.sendWelcomeEmail(email, nickname);
            log.info("환영 이메일 발송 요청 완료 - email: {}, nickname: {}", email, nickname);
        } catch (Exception e) {
            log.warn("환영 이메일 발송 요청 실패 - email: {}, nickname: {}, error: {}",
                    email, nickname, e.getMessage());
        }
    }
}
