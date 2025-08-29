package com.toktot.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailSendResponse(
        @JsonProperty("email")
        String email,

        @JsonProperty("message")
        String message,

        @JsonProperty("expires_in_minutes")
        Integer expiresInMinutes
) {
    public static EmailSendResponse success(String email, String message) {
        return new EmailSendResponse(email, message, 5);
    }
}

