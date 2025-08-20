package com.toktot.web.dto.auth.request.password;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @JsonProperty(value = "email", required = true)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @JsonProperty(value = "new_password", required = true)
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 30, message = "비밀번호는 8-30자 사이여야 합니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
        String newPassword
) {
}
