package com.toktot.web.dto.auth.reqeust;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @JsonProperty(value = "code", required = true)
        @NotBlank(message = "카카오 인증 코드는 필수입니다.")
        String code
) {
}
