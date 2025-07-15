package com.toktot.web.dto.auth.request.password;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record PasswordResetCompleteRequest(
        @JsonProperty(value = "email", required = true)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @JsonProperty(value = "reset_token", required = true)
        @NotBlank(message = "인증번호는 필수입니다.")
        String verificationCode,

        @JsonProperty(value = "new_password", required = true)
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 30, message = "비밀번호는 8-30자 사이여야 합니다.")
        String newPassword
) {
}
