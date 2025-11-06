package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiResponse<T>(
        @JsonProperty("response")
        TourApiResponseBody<T> response
) {

    public record TourApiResponseBody<T>(
            @JsonProperty("header")
            TourApiHeader header,

            @JsonProperty("body")
            TourApiBody<T> body
    ) {}

    public record TourApiHeader(
            @JsonProperty("resultCode")
            String resultCode,

            @JsonProperty("resultMsg")
            String resultMsg
    ) {}

    public record TourApiBody<T>(
            @JsonProperty("items")
            T items,

            @JsonProperty("numOfRows")
            Integer numOfRows,

            @JsonProperty("pageNo")
            Integer pageNo,

            @JsonProperty("totalCount")
            Integer totalCount
    ) {}
}
