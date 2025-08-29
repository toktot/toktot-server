package com.toktot.domain.user.dto.mapper;

import com.toktot.domain.user.User;
import com.toktot.domain.user.type.AuthProvider;
import com.toktot.domain.user.dto.request.register.RegisterCompleteRequest;
import com.toktot.domain.user.dto.response.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRequestMapper {

    private final PasswordEncoder passwordEncoder;

    public User toUserEntity(RegisterCompleteRequest request) {
        if (request == null) {
            log.warn("RegisterCompleteRequest가 null입니다.");
            return null;
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        return User.builder()
                .email(request.email().toLowerCase().trim())
                .password(encodedPassword)
                .authProvider(AuthProvider.EMAIL)
                .nickname(request.nickname().trim())
                .build();
    }

    public User toUserEntity(KakaoUserInfoResponse kakaoUserInfo) {
        if (kakaoUserInfo == null) {
            log.warn("KakaoUserInfoResponse가 null입니다.");
            return null;
        }

        String nickname = determineNickname(kakaoUserInfo);

        return User.builder()
                .oauthId(kakaoUserInfo.id())
                .authProvider(AuthProvider.KAKAO)
                .nickname(nickname)
                .profileImageUrl(kakaoUserInfo.profileImageUrl())
                .build();
    }

    private String determineNickname(KakaoUserInfoResponse kakaoUserInfo) {
        if (kakaoUserInfo.hasValidNickname()) {
            return kakaoUserInfo.nickname().trim();
        }

        String defaultNickname = "카카오사용자" +
                kakaoUserInfo.id().substring(Math.max(0, kakaoUserInfo.id().length() - 4));

        log.debug("기본 닉네임 생성 - kakaoId: {}, nickname: {}",
                kakaoUserInfo.id(), defaultNickname);

        return defaultNickname;
    }
}
