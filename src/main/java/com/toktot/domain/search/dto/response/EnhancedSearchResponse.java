package com.toktot.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.localfood.dto.LocalFoodStatsResponse;
import lombok.Builder;

/**
 * 향토음식 통계 포함 검색 응답
 */
@Builder
public record EnhancedSearchResponse<T>(
        T data,

        @JsonProperty("local_food_stats")
        LocalFoodStatsResponse localFoodStats,

        @JsonProperty("is_local_food_search")
        boolean isLocalFoodSearch
) {

    public static <T> EnhancedSearchResponse<T> withLocalFood(T data, LocalFoodStatsResponse stats) {
        return EnhancedSearchResponse.<T>builder()
                .data(data)
                .localFoodStats(stats)
                .isLocalFoodSearch(true)
                .build();
    }

    public static <T> EnhancedSearchResponse<T> withoutLocalFood(T data) {
        return EnhancedSearchResponse.<T>builder()
                .data(data)
                .localFoodStats(null)
                .isLocalFoodSearch(false)
                .build();
    }
}
