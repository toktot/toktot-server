package com.toktot.external.jeju.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class JejuGoodPriceResponse {

    @JsonProperty("response")
    private Response response;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Response {
        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Header {
        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Body {
        @JsonProperty("pageNo")
        private Integer pageNo;

        @JsonProperty("totalCount")
        private Integer totalCount;

        @JsonProperty("numOfRows")
        private Integer numOfRows;

        @JsonProperty("items")
        private Items items;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Items {
        @JsonProperty("item")
        private List<JejuGoodPriceItem> item;
    }

    public List<JejuGoodPriceItem> getItems() {
        if (response != null && response.body != null && response.body.items != null) {
            return response.body.items.item != null ? response.body.items.item : List.of();
        }
        return List.of();
    }

    public Integer getTotalCount() {
        if (response != null && response.body != null) {
            return response.body.totalCount;
        }
        return 0;
    }

    public boolean isSuccess() {
        return response != null &&
               response.header != null &&
               "00".equals(response.header.resultCode);
    }
}
