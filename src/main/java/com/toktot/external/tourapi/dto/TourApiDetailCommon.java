package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiDetailCommon(
        @JsonProperty("contentid") String id,
        @JsonProperty("tel") String phone,
        @JsonProperty("homepage") String url,
        @JsonProperty("firstimage") String image,
        @JsonProperty("firstimage2") String image2
) {

}
