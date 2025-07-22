package com.toktot.domain.user.service;

import com.toktot.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailAuthService {

    private final UserValidationService userValidationService;
    private final UserRegistrationService userRegistrationService;
    private final LoginService loginService;

    public void checkEmailDuplicate(String email) {
        userValidationService.validateEmailAvailability(email);
        log.debug("이메일 중복 검증 완료 - email: {}", email);
    }

    public void checkNicknameDuplicate(String nickname) {
        userValidationService.validateNicknameAvailability(nickname);
        log.debug("닉네임 중복 검증 완료 - nickname: {}", nickname);
    }

    public User registerEmailUser(String email, String password, String nickname,
                                  String clientIp, String userAgent) {
        log.debug("이메일 회원가입 요청 위임 - email: {}, nickname: {}", email, nickname);
        return userRegistrationService.registerEmailUser(email, password, nickname, clientIp, userAgent);
    }

    public User authenticateEmailUser(String email, String password, String clientIp) {
        log.debug("이메일 로그인 인증 요청 위임 - email: {}", email);
        return loginService.authenticateEmailUser(email, password, clientIp);
    }
}
