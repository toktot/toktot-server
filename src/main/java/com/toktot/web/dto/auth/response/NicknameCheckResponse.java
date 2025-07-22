package com.toktot.web.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NicknameCheckResponse(
        @JsonProperty("nickname")
        String nickname,

        @JsonProperty("available")
        boolean available,

        @JsonProperty("message")
        String message
) {
    public static NicknameCheckResponse available(String nickname) {
        return new NicknameCheckResponse(nickname, true, "사용 가능한 닉네임입니다.");
    }

    public static NicknameCheckResponse unavailable(String nickname) {
        return new NicknameCheckResponse(nickname, false, "이미 사용중인 닉네임입니다.");
    }
}
