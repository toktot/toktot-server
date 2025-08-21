package com.toktot.external.kakao.exception;

import lombok.Getter;

@Getter
public class KakaoApiException extends RuntimeException {

    private final String errorCode;

    public KakaoApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public KakaoApiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return String.format("KakaoApiException{errorCode='%s', message='%s'}",
                errorCode, getMessage());
    }
}
