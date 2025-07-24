package com.toktot.web.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailCheckResponse(
        @JsonProperty("email")
        String email,

        @JsonProperty("available")
        boolean available,

        @JsonProperty("message")
        String message
) {
    public static EmailCheckResponse available(String email) {
        return new EmailCheckResponse(email, true, "사용 가능한 이메일이에요.");
    }

    public static EmailCheckResponse unavailable(String email) {
        return new EmailCheckResponse(email, false, "이미 사용한 이메일이에요.");
    }
}
