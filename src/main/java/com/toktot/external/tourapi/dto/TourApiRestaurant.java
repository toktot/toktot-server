package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiRestaurant(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        @JsonProperty("title")
        String title,

        @JsonProperty("addr1")
        String address1,

        @JsonProperty("addr2")
        String address2,

        @JsonProperty("areacode")
        String areaCode,

        @JsonProperty("cat1")
        String category1,

        @JsonProperty("cat2")
        String category2,

        @JsonProperty("cat3")
        String category3,

        @JsonProperty("mapx")
        String longitude,

        @JsonProperty("mapy")
        String latitude,

        @JsonProperty("tel")
        String phoneNumber,

        @JsonProperty("firstimage")
        String firstImage,

        @JsonProperty("firstimage2")
        String firstImage2,

        @JsonProperty("readcount")
        Integer readCount,

        @JsonProperty("sigungucode")
        String sigunguCode,

        @JsonProperty("zipcode")
        String zipCode,

        @JsonProperty("modifiedtime")
        String modifiedTime,

        @JsonProperty("createdtime")
        String createdTime,

        @JsonProperty("booktour")
        String bookTour,

        @JsonProperty("mlevel")
        String mLevel
) {

}
