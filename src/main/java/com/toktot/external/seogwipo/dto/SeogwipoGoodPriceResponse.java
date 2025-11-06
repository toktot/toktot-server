package com.toktot.external.seogwipo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SeogwipoGoodPriceResponse {

        @JsonProperty("currentCount")
        private Integer currentCount;

        @JsonProperty("data")
        private List<SeogwipoGoodPriceItem> data;

        @JsonProperty("matchCount")
        private Integer matchCount;

        @JsonProperty("page")
        private Integer page;

        @JsonProperty("perPage")
        private Integer perPage;

        @JsonProperty("totalCount")
        private Integer totalCount;

        public List<SeogwipoGoodPriceItem> getItems() {
                return data;
        }
}
