package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiDetailCommon(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("homepage")
        String homepage,

        @JsonProperty("overview")
        String overview,

        @JsonProperty("tel")
        String tel,

        @JsonProperty("telname")
        String telName,

        @JsonProperty("title")
        String title,

        @JsonProperty("firstimage")
        String firstImage,

        @JsonProperty("firstimage2")
        String firstImage2,

        @JsonProperty("areacode")
        String areaCode,

        @JsonProperty("sigungucode")
        String sigunguCode,

        @JsonProperty("addr1")
        String addr1,

        @JsonProperty("addr2")
        String addr2,

        @JsonProperty("mapx")
        String mapX,

        @JsonProperty("mapy")
        String mapY,

        @JsonProperty("zipcode")
        String zipCode,

        @JsonProperty("modifiedtime")
        String modifiedTime,

        @JsonProperty("createdtime")
        String createdTime
) {

}
