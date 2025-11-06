package com.toktot.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record KakaoUserInfoResponse(
        @JsonProperty("id")
        String id,

        @JsonProperty("nickname")
        String nickname,

        @JsonProperty("profile_image_url")
        String profileImageUrl
) {
    public static KakaoUserInfoResponse from(String kakaoId, String nickname, String profileImageUrl) {
        return KakaoUserInfoResponse.builder()
                .id(kakaoId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public boolean hasValidNickname() {
        return nickname != null && !nickname.trim().isEmpty();
    }
}
