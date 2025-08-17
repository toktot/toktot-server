package com.toktot.external.tourapi.exception;

import lombok.Getter;

@Getter
public class TourApiException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public TourApiException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public TourApiException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
