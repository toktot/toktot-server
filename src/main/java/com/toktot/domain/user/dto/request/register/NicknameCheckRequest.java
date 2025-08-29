package com.toktot.domain.user.dto.request.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NicknameCheckRequest(
        @JsonProperty(value = "nickname", required = true)
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 6, max = 20, message = "닉네임은 6-20자 사이여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
        String nickname
) {
}
