package com.toktot.domain.user.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthProvider {
    EMAIL("이메일"),
    KAKAO("카카오");

    private final String description;
}
