package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserValidationService {

    private final UserRepository userRepository;

    public void validateEmailAvailability(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("이메일 중복 검증 실패 - email: {}", email);
            throw new ToktotException(ErrorCode.DUPLICATE_EMAIL);
        }

        log.debug("이메일 중복 검증 통과 - email: {}", email);
    }

    public void validateNicknameAvailability(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            log.warn("닉네임 중복 검증 실패 - nickname: {}", nickname);
            throw new ToktotException(ErrorCode.DUPLICATE_USERNAME);
        }

        log.debug("닉네임 중복 검증 통과 - nickname: {}", nickname);
    }

    public boolean isEmailAvailable(String email) {
        boolean available = !userRepository.existsByEmail(email);
        log.debug("이메일 사용 가능 여부 확인 - email: {}, available: {}", email, available);
        return available;
    }

    public boolean isNicknameAvailable(String nickname) {
        boolean available = !userRepository.existsByNickname(nickname);
        log.debug("닉네임 사용 가능 여부 확인 - nickname: {}, available: {}", nickname, available);
        return available;
    }
}
