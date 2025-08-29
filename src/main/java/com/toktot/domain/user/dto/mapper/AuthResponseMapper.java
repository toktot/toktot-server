package com.toktot.domain.user.dto.mapper;

import com.toktot.domain.user.User;
import com.toktot.domain.user.dto.response.EmailCheckResponse;
import com.toktot.domain.user.dto.response.EmailSendResponse;
import com.toktot.domain.user.dto.response.NicknameCheckResponse;
import com.toktot.domain.user.dto.response.TokenResponse;
import com.toktot.domain.user.dto.response.password.PasswordResetVerifyResponse;
import com.toktot.domain.user.dto.response.password.PasswordUpdateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthResponseMapper {

    public EmailSendResponse toEmailSendResponse(String email, String message) {
        log.debug("이메일 발송 응답 생성 - email: {}, message: {}", email, message);
        return EmailSendResponse.success(email, message);
    }

    public NicknameCheckResponse toNicknameAvailableResponse(String nickname) {
        log.debug("닉네임 사용 가능 응답 생성 - nickname: {}", nickname);
        return NicknameCheckResponse.available(nickname);
    }

    public NicknameCheckResponse toNicknameUnavailableResponse(String nickname) {
        log.debug("닉네임 사용 불가 응답 생성 - nickname: {}", nickname);
        return NicknameCheckResponse.unavailable(nickname);
    }

    public TokenResponse toPublicTokenResponse(TokenResponse originalTokenResponse) {
        if (originalTokenResponse == null) {
            log.warn("TokenResponse가 null입니다.");
            return null;
        }

        TokenResponse publicResponse = TokenResponse.of(
                originalTokenResponse.accessToken(),
                null,
                originalTokenResponse.expiresIn()
        );

        log.debug("공개용 토큰 응답 생성 완료 - expiresIn: {}초", originalTokenResponse.expiresIn());
        return publicResponse;
    }

    public String toLoginSuccessMessage(User user) {
        if (user == null) {
            return "로그인이 완료되었습니다.";
        }

        String message = switch (user.getAuthProvider()) {
            case EMAIL -> "이메일 로그인이 완료되었습니다.";
            case KAKAO -> "카카오 로그인이 완료되었습니다.";
        };

        log.debug("로그인 성공 메시지 생성 - userId: {}, provider: {}, message: {}",
                user.getId(), user.getAuthProvider(), message);

        return message;
    }

    public String toRegistrationSuccessMessage(User user) {
        if (user == null) {
            return "회원가입이 완료되었습니다.";
        }

        String message = String.format("%s님, 회원가입이 완료되었습니다.", user.getNickname());

        log.debug("회원가입 성공 메시지 생성 - userId: {}, nickname: {}, message: {}",
                user.getId(), user.getNickname(), message);

        return message;
    }

    public EmailCheckResponse toEmailAvailableResponse(String email) {
        log.debug("이메일 사용 가능 응답 생성 - email: {}", email);
        return EmailCheckResponse.available(email);
    }

    public EmailCheckResponse toEmailUnavailableResponse(String email) {
        log.debug("이메일 사용 불가 응답 생성 - email: {}", email);
        return EmailCheckResponse.unavailable(email);
    }

    public PasswordResetVerifyResponse toPasswordResetVerifyResponse(String email) {
        log.debug("비밀번호 재설정 인증 응답 생성 - email: {}", email);
        return PasswordResetVerifyResponse.success(email);
    }

    public PasswordUpdateResponse toPasswordUpdateResponse(String email) {
        log.debug("비밀번호 업데이트 응답 생성 - email: {}", email);
        return PasswordUpdateResponse.success(email);
    }
}
