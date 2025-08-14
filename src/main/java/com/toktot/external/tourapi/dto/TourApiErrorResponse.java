package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiErrorResponse(
        @JsonProperty("response")
        ErrorResponseBody response
) {

    public record ErrorResponseBody(
            @JsonProperty("header")
            ErrorHeader header
    ) {}

    public record ErrorHeader(
            @JsonProperty("resultCode")
            String resultCode,

            @JsonProperty("resultMsg")
            String resultMsg
    ) {}
}
