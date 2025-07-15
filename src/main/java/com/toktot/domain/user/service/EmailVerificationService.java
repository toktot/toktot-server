package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private static final String VERIFICATION_STATUS_PREFIX = "verification_status:";
    private static final String SEND_COOLDOWN_PREFIX = "send_cooldown:";

    private static final Duration VERIFICATION_CODE_EXPIRY = Duration.ofMinutes(5);
    private static final Duration VERIFICATION_STATUS_EXPIRY = Duration.ofMinutes(30);
    private static final Duration SEND_COOLDOWN = Duration.ofMinutes(1);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void sendVerificationCode(String email) {
        validateSendCooldown(email);

        String verificationCode = generateVerificationCode();

        storeVerificationCode(email, verificationCode);
        setSendCooldown(email);

        emailService.sendVerificationEmail(email, verificationCode);

        log.info("이메일 인증 코드 발송 완료 - email: {}", email);
    }

    public void verifyCode(String email, String inputCode) {
        String storedCode = getStoredVerificationCode(email);

        if (storedCode == null) {
            log.warn("인증 코드가 만료되거나 존재하지 않음 - email: {}", email);
            throw new ToktotException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!storedCode.equals(inputCode)) {
            log.warn("인증 코드 불일치 - email: {}, inputCode: {}", email, inputCode);
            throw new ToktotException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        markAsVerified(email);
        clearVerificationCode(email);

        log.info("이메일 인증 성공 - email: {}", email);
    }

    public boolean isVerified(String email) {
        String key = VERIFICATION_STATUS_PREFIX + email;
        String status = redisTemplate.opsForValue().get(key);

        boolean verified = "verified".equals(status);
        log.debug("이메일 인증 상태 확인 - email: {}, verified: {}", email, verified);

        return verified;
    }

    public void clearVerificationStatus(String email) {
        String statusKey = VERIFICATION_STATUS_PREFIX + email;
        redisTemplate.delete(statusKey);

        log.debug("이메일 인증 상태 삭제 - email: {}", email);
    }

    private void validateSendCooldown(String email) {
        String cooldownKey = SEND_COOLDOWN_PREFIX + email;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            log.warn("인증 코드 재발송 제한 시간 미경과 - email: {}", email);
            throw new ToktotException(ErrorCode.VERIFICATION_CODE_ALREADY_SENT);
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private void storeVerificationCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, VERIFICATION_CODE_EXPIRY);

        log.debug("인증 코드 저장 완료 - email: {}, expiry: {}분", email, VERIFICATION_CODE_EXPIRY.toMinutes());
    }

    private void setSendCooldown(String email) {
        String cooldownKey = SEND_COOLDOWN_PREFIX + email;
        redisTemplate.opsForValue().set(cooldownKey, "sent", SEND_COOLDOWN);

        log.debug("발송 제한 설정 - email: {}, cooldown: {}분", email, SEND_COOLDOWN.toMinutes());
    }

    private String getStoredVerificationCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    private void markAsVerified(String email) {
        String statusKey = VERIFICATION_STATUS_PREFIX + email;
        redisTemplate.opsForValue().set(statusKey, "verified", VERIFICATION_STATUS_EXPIRY);

        log.debug("이메일 인증 완료 상태 저장 - email: {}, expiry: {}분",
                email, VERIFICATION_STATUS_EXPIRY.toMinutes());
    }

    private void clearVerificationCode(String email) {
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.delete(codeKey);

        log.debug("사용된 인증 코드 삭제 - email: {}", email);
    }
}
