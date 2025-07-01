package com.toktot.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 회원(사용자) 관련 에러 (USER)
    DUPLICATE_EMAIL("이미 사용중인 이메일입니다"),
    ;

    private final String message;

}
