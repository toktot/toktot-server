package com.toktot.domain.restaurant.dto.request;

import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.request.LocationFilterRequest;

public record RestaurantSearchRequest(
        String query,
        LocationFilterRequest location,
        Integer page,
        SortType sort
) {

    public boolean hasQuery() {
        return this.query != null && !this.query.isBlank();
    }

    public boolean hasPage() {
        return this.page != null && this.page > 0;
    }
}
