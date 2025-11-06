package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
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
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User authenticateEmailUser(String email, String password, String clientIp) {
        User user = findEmailUser(email, clientIp);

        validatePassword(user, password, email, clientIp);
        validateUserAccountStatus(user, email, clientIp);

        handleSuccessfulLogin(user, clientIp);

        log.info("이메일 로그인 성공 - userId: {}, email: {}, clientIp: {}",
                user.getId(), email, clientIp);

        return user;
    }

    private User findEmailUser(String email, String clientIp) {
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

        return user;
    }

    private void validatePassword(User user, String password, String email, String clientIp) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("이메일 로그인 실패 - 비밀번호 불일치: {}, clientIp: {}", email, clientIp);
            handleFailedLogin(user, clientIp);
            throw new ToktotException(ErrorCode.INVALID_PASSWORD);
        }
    }

    private void validateUserAccountStatus(User user, String email, String clientIp) {
        if (user.isDeleted()) {
            log.warn("이메일 로그인 실패 - 탈퇴한 계정: {}, clientIp: {}", email, clientIp);
            throw new ToktotException(ErrorCode.ACCOUNT_DISABLED);
        }

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
}
