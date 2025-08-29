package com.toktot.domain.user.dto.request.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record RegisterCompleteRequest(
        @JsonProperty(value = "email", required = true)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @JsonProperty(value = "password", required = true)
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다.")
        String password,

        @JsonProperty(value = "nickname", required = true)
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 20, message = "닉네임은 1-20자 사이여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
        String nickname
) {
}
