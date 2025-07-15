package com.toktot.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toktot.common.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        ErrorCode errorCode,
        T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getMessage(), errorCode, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customErrorMessage) {
        return new ApiResponse<>(false, customErrorMessage, errorCode, null);
    }
}
