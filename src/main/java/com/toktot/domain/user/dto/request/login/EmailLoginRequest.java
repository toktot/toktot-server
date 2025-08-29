package com.toktot.domain.user.dto.request.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailLoginRequest(
        @JsonProperty(value = "email", required = true)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @JsonProperty(value = "password", required = true)
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
