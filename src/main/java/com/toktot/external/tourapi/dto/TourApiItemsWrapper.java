package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TourApiItemsWrapper(
        @JsonProperty("item")
        List<TourApiRestaurant> item
) {
}
