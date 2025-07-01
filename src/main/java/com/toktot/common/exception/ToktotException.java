package com.toktot.common.exception;

import lombok.Getter;

@Getter
public class ToktotException extends RuntimeException {

    private final ErrorCode errorCode;

    public ToktotException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ToktotException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ToktotException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ToktotException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCodeName() {
        return errorCode.name();
    }

    public boolean isAuthError() {
        return errorCode.isAuthError();
    }

    public boolean isValidationError() {
        return errorCode.isValidationError();
    }

    public boolean isPermissionError() {
        return errorCode.isPermissionError();
    }

    public boolean isSystemError() {
        return errorCode.isSystemError();
    }

    @Override
    public String toString() {
        return String.format("ToktotException{errorCode=%s, message='%s'}",
                errorCode.name(), getMessage());
    }
}
