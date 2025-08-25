package com.toktot.external.tourapi.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.external.tourapi.dto.TourApiDetailIntro;

import java.util.List;

public record TourApiDetailIntroWrapper(
        @JsonProperty("item") List<TourApiDetailIntro> item
) {

    public TourApiDetailIntro getFirstItem() {
        return (item != null && !item.isEmpty()) ? item.get(0) : null;
    }
}
