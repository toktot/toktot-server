package com.toktot.domain.user.dto.response.password;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordResetVerifyResponse(
        @JsonProperty("email")
        String email,

        @JsonProperty("verified")
        boolean verified,

        @JsonProperty("expires_in_minutes")
        int expiresInMinutes,

        @JsonProperty("message")
        String message
) {
    public static PasswordResetVerifyResponse success(String email) {
        return new PasswordResetVerifyResponse(
                email,
                true,
                10,
                "인증이 완료되었습니다. 새로운 비밀번호를 설정해주세요."
        );
    }
}
