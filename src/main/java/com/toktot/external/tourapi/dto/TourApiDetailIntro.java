package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiDetailIntro(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        @JsonProperty("discountinfofood")
        String discountInfo,

        @JsonProperty("firstmenu")
        String firstMenu,

        @JsonProperty("infocenterfood")
        String infoCenter,

        @JsonProperty("kidsfacility")
        String kidsFacility,

        @JsonProperty("opendatefood")
        String openDate,

        @JsonProperty("opentimefood")
        String openTime,

        @JsonProperty("packing")
        String packing,

        @JsonProperty("parkingfood")
        String parking,

        @JsonProperty("reservationfood")
        String reservation,

        @JsonProperty("restdatefood")
        String restDate,

        @JsonProperty("scalefood")
        String scale,

        @JsonProperty("seat")
        String seat,

        @JsonProperty("smoking")
        String smoking,

        @JsonProperty("treatmenu")
        String treatMenu,

        @JsonProperty("lcnsno")
        String licenseNumber
) {

}
