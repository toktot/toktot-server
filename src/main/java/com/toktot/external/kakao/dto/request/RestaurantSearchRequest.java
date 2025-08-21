package com.toktot.external.kakao.dto.request;

import java.math.BigDecimal;

public record RestaurantSearchRequest(
        String query,
        BigDecimal longitude,
        BigDecimal latitude,
        Integer radius,
        Integer page,
        String sort
) {

    public RestaurantSearchRequest nextPage() {
        return new RestaurantSearchRequest(query, longitude, latitude, radius, page + 1, sort);
    }
}
