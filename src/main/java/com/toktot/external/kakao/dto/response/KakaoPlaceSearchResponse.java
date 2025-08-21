package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.external.kakao.KakaoApiConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoPlaceSearchResponse(
        @JsonProperty("documents")
        List<KakaoPlaceInfo> placeInfos,

        @JsonProperty("meta")
        KakaoMeta meta,

        @JsonProperty("current_page")
        Integer currentPage,

        @JsonProperty("is_end")
        Boolean isEnd
) {

    public KakaoPlaceSearchResponse addPlaceInfo(KakaoPlaceSearchResponse beforeResponse) {
        if (beforeResponse == null || beforeResponse.placeInfos == null) {
            return this;
        }

        if (this.placeInfos == null) {
            return beforeResponse;
        }

        List<KakaoPlaceInfo> mergedPlaceInfos = new ArrayList<>();
        mergedPlaceInfos.addAll(beforeResponse.placeInfos);
        mergedPlaceInfos.addAll(this.placeInfos);

        return new KakaoPlaceSearchResponse(
                mergedPlaceInfos,
                this.meta,
                this.currentPage,
                this.isEnd
        );
    }

    public KakaoPlaceSearchResponse filterFoodAndCafe() {
        List<KakaoPlaceInfo> filteredPlaces = placeInfos.stream()
                .filter(place -> KakaoApiConstants.CATEGORY_FOOD.equals(place.getCategoryGroupCode()) ||
                        KakaoApiConstants.CATEGORY_CAFE.equals(place.getCategoryGroupCode()))
                .toList();

        return new KakaoPlaceSearchResponse(filteredPlaces, meta, 0, null);
    }

    public boolean hasResults() {
        return placeInfos != null && !placeInfos.isEmpty();
    }

    public int getResultCount() {
        if (placeInfos != null) {
            return placeInfos.size();
        }

        return 0;
    }

    public boolean hasMorePages() {
        return meta != null && !meta.isEnd();
    }

    public List<KakaoPlaceInfo> getPlaceInfos() {
        if (placeInfos != null) {
            return placeInfos;
        }

        return Collections.emptyList();
    }
}

