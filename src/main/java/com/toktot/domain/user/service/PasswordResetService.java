package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.domain.user.type.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String RESET_TOKEN_PREFIX = "password_reset:";
    private static final String RESET_COOLDOWN_PREFIX = "reset_cooldown:";

    private static final Duration RESET_TOKEN_EXPIRY = Duration.ofHours(1);
    private static final Duration RESET_COOLDOWN = Duration.ofMinutes(1);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void sendPasswordResetCode(String email) {
        validateEmailUser(email);
        validateResetCooldown(email);

        String resetToken = generateResetToken();

        storeResetToken(email, resetToken);
        setResetCooldown(email);

        emailService.sendPasswordResetEmail(email, resetToken);

        log.info("비밀번호 재설정 코드 발송 완료 - email: {}", email);
    }

    public void resetPassword(String email, String resetToken, String newPassword) {
        validateResetToken(email, resetToken);

        User user = findEmailUser(email);
        updateUserPassword(user, newPassword);

        clearResetToken(email);

        log.info("비밀번호 재설정 완료 - userId: {}, email: {}", user.getId(), email);
    }

    private void validateEmailUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("비밀번호 재설정 요청 - 존재하지 않는 이메일: {}", email);
                    return new ToktotException(ErrorCode.USER_NOT_FOUND);
                });

        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            log.warn("비밀번호 재설정 요청 - 이메일 계정이 아님: {}, provider: {}", email, user.getAuthProvider());
            throw new ToktotException(ErrorCode.OPERATION_NOT_ALLOWED, "소셜 계정은 비밀번호 재설정이 불가능합니다.");
        }
    }

    private void validateResetCooldown(String email) {
        String cooldownKey = RESET_COOLDOWN_PREFIX + email;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            log.warn("비밀번호 재설정 코드 재발송 제한 시간 미경과 - email: {}", email);
            throw new ToktotException(ErrorCode.VERIFICATION_CODE_ALREADY_SENT,
                    "비밀번호 재설정 코드가 이미 발송되었습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private String generateResetToken() {
        return String.format("%06d", SECURE_RANDOM.nextInt(500000));
    }

    private void storeResetToken(String email, String token) {
        String key = RESET_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(key, token, RESET_TOKEN_EXPIRY);

        log.debug("비밀번호 재설정 토큰 저장 완료 - email: {}, expiry: {}분", email, RESET_TOKEN_EXPIRY.toMinutes());
    }

    private void setResetCooldown(String email) {
        String cooldownKey = RESET_COOLDOWN_PREFIX + email;
        redisTemplate.opsForValue().set(cooldownKey, "sent", RESET_COOLDOWN);

        log.debug("재설정 발송 제한 설정 - email: {}, cooldown: {}분", email, RESET_COOLDOWN.toMinutes());
    }

    private void validateResetToken(String email, String inputToken) {
        String key = RESET_TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null) {
            log.warn("비밀번호 재설정 토큰이 만료되거나 존재하지 않음 - email: {}", email);
            throw new ToktotException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        if (!storedToken.equals(inputToken)) {
            log.warn("비밀번호 재설정 토큰 불일치 - email: {}, inputToken: {}", email, inputToken);
            throw new ToktotException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
    }

    private User findEmailUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("비밀번호 재설정 중 사용자 조회 실패 - email: {}", email);
                    return new ToktotException(ErrorCode.USER_NOT_FOUND);
                });
    }

    private void updateUserPassword(User user, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);

        user.updatePassword(encodedPassword);

        if (user.getUserProfile() != null) {
            user.getUserProfile().updatePasswordChangedAt();
        }

        userRepository.save(user);

        log.debug("사용자 비밀번호 업데이트 완료 - userId: {}", user.getId());
    }

    private void clearResetToken(String email) {
        String key = RESET_TOKEN_PREFIX + email;
        redisTemplate.delete(key);

        log.debug("사용된 재설정 토큰 삭제 - email: {}", email);
    }
}
