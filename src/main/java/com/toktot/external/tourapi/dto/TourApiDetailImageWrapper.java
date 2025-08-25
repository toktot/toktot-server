package com.toktot.external.tourapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiDetailImageWrapper(
        List<TourApiDetailImage> item
) {
    public TourApiDetailImage getFirstItem() {
        return (item != null && !item.isEmpty()) ? item.get(0) : null;
    }
}
