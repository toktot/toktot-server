package com.toktot.domain.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record RestaurantSearchResponse(
        List<RestaurantInfoResponse> places,

        @JsonProperty("current_page")
        Integer currentPage,

        @JsonProperty("is_end")
        Boolean is_end
) {

    public static RestaurantSearchResponse from(List<RestaurantInfoResponse> places, Integer currentPage, Boolean is_end) {
        return new RestaurantSearchResponse(places, currentPage, is_end);
    }

    public static RestaurantSearchResponse from(KakaoPlaceSearchResponse response, Integer page) {
        return RestaurantSearchResponse.builder()
                .places(response.getPlaceInfos()
                        .stream()
                        .map(RestaurantInfoResponse::from)
                        .toList())
                .currentPage(page)
                .is_end(response.meta().isEnd())
                .build();
    }
}
