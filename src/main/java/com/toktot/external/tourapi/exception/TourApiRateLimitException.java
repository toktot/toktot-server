package com.toktot.external.tourapi.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TourApiRateLimitException extends TourApiException {
    private final int remainingCalls;
    private final LocalDateTime resetTime;

    public TourApiRateLimitException(String message, int remainingCalls, LocalDateTime resetTime) {
        super(message, "RATE_LIMIT_EXCEEDED", 429);
        this.remainingCalls = remainingCalls;
        this.resetTime = resetTime;
    }
}
