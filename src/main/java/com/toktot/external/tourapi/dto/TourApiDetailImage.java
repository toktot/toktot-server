package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiDetailImage(
        String contentid,
        String originimgurl,
        String imgname,
        String smallimageurl,
        String cpyrhtDivCd,
        String serialnum
) {}
