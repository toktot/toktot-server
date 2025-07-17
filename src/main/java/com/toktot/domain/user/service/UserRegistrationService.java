package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.UserAgreement;
import com.toktot.domain.user.UserProfile;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.web.dto.auth.request.register.RegisterCompleteRequest;
import com.toktot.web.mapper.AuthRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserValidationService userValidationService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final AuthRequestMapper authRequestMapper;

    public User registerEmailUser(String email, String password, String nickname,
                                  String clientIp, String userAgent) {

        validateRegistrationPreconditions(email, nickname);

        RegisterCompleteRequest request = new RegisterCompleteRequest(email, password, nickname);
        User newUser = authRequestMapper.toUserEntity(request);
        User savedUser = userRepository.save(newUser);

        setupUserRelations(savedUser, clientIp, userAgent);
        User finalUser = userRepository.save(savedUser);

        finalizeRegistration(email, nickname);

        log.info("이메일 회원가입 완료 - userId: {}, email: {}, nickname: {}, clientIp: {}",
                finalUser.getId(), email, nickname, clientIp);

        return finalUser;
    }

    public User registerEmailUser(RegisterCompleteRequest request, String clientIp, String userAgent) {
        return registerEmailUser(request.email(), request.password(), request.nickname(), clientIp, userAgent);
    }

    private void validateRegistrationPreconditions(String email, String nickname) {
        userValidationService.validateEmailAvailability(email);
        userValidationService.validateNicknameAvailability(nickname);

        if (!emailVerificationService.isVerified(email)) {
            log.warn("회원가입 실패 - 이메일 인증 미완료: {}", email);
            throw new ToktotException(ErrorCode.EMAIL_NOT_VERIFIED, "이메일 인증을 완료해주세요.");
        }

        log.debug("회원가입 사전 조건 검증 완료 - email: {}, nickname: {}", email, nickname);
    }

    private void setupUserRelations(User user, String clientIp, String userAgent) {
        UserProfile userProfile = UserProfile.createDefault(user);
        UserAgreement userAgreement = UserAgreement.createWithFullAgreement(user, clientIp, userAgent);

        user.assignUserProfile(userProfile);
        user.assignUserAgreement(userAgreement);

        log.debug("사용자 연관관계 설정 완료 - userId: {}", user.getId());
    }

    private void finalizeRegistration(String email, String nickname) {
        emailVerificationService.clearVerificationStatus(email);
        sendWelcomeEmailAsync(email, nickname);
    }

    private void sendWelcomeEmailAsync(String email, String nickname) {
        try {
            emailService.sendWelcomeEmail(email, nickname);
            log.info("환영 이메일 발송 요청 완료 - email: {}, nickname: {}", email, nickname);
        } catch (Exception e) {
            log.warn("환영 이메일 발송 요청 실패 - email: {}, nickname: {}, error: {}",
                    email, nickname, e.getMessage());
        }
    }
}
