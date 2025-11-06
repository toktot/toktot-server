package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TourApiDetailIntro(
        @JsonProperty("contentid") String contentId,
        @JsonProperty("contenttypeid") String contentTypeId,
        @JsonProperty("seat") String seat,
        @JsonProperty("kidsfacility") String kidsFacility,
        @JsonProperty("firstmenu") String firstMenu,
        @JsonProperty("treatmenu") String treatMenu,
        @JsonProperty("smoking") String smoking,
        @JsonProperty("packing") String packing,
        @JsonProperty("infocenterfood") String infoCenterFood,
        @JsonProperty("scalefood") String scaleFood,
        @JsonProperty("parkingfood") String parkingFood,
        @JsonProperty("opendatefood") String openDateFood,
        @JsonProperty("opentimefood") String openTimeFood,
        @JsonProperty("restdatefood") String restDateFood,
        @JsonProperty("discountinfofood") String discountInfoFood,
        @JsonProperty("chkcreditcardfood") String chkCreditCardFood,
        @JsonProperty("reservationfood") String reservationFood,
        @JsonProperty("lcnsno") String licenseNo
) {

}