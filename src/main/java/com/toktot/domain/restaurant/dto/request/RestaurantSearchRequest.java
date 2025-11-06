package com.toktot.domain.restaurant.dto.request;

import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.request.LocationFilterRequest;

public record RestaurantSearchRequest(
        String query,
        LocationFilterRequest location,
        Integer page,
        SortType sort
) {

    public Double latitude() {
        return location != null ? location.latitude() : null;
    }

    public Double longitude() {
        return location != null ? location.longitude() : null;
    }

    public boolean hasQuery() {
        return this.query != null && !this.query.isBlank();
    }

    public boolean hasPage() {
        return this.page != null && this.page > 0;
    }
}
