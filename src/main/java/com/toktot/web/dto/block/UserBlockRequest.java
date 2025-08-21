package com.toktot.web.dto.block;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UserBlockRequest(
        @JsonProperty("blocked_user_id")
        @NotNull(message = "차단할 사용자가 선택되지 않았습니다.")
        Long blockedUserId
) {
}
