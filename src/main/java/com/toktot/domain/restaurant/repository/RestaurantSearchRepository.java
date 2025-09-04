package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.dto.response.RestaurantInfoResponse;
import com.toktot.web.dto.request.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RestaurantSearchRepository {

    Page<RestaurantInfoResponse> searchRestaurantsWithFilters(
            SearchCriteria criteria,
            Long currentUserId,
            List<Long> blockedUserIds,
            Pageable pageable
    );
}
