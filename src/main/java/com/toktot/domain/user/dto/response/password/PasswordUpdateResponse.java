package com.toktot.domain.user.dto.response.password;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PasswordUpdateResponse(
        @JsonProperty("email")
        String email,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("message")
        String message
) {
    public static PasswordUpdateResponse success(String email) {
        return new PasswordUpdateResponse(
                email,
                LocalDateTime.now(),
                "비밀번호가 성공적으로 변경되었습니다."
        );
    }
}
