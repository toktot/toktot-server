package com.toktot.external.kakao.dto.request;

import com.toktot.external.kakao.KakaoApiConstants;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class KakaoPlaceSearchRequest {

    private String query;
    private String categoryGroupCode;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer radius;
    private Integer page;
    private Integer size;
    private String sort;

    public static KakaoPlaceSearchRequest keyword(String query) {
        return KakaoPlaceSearchRequest.builder()
                .query(query)
                .page(KakaoApiConstants.DEFAULT_PAGE)
                .size(15)
                .sort(KakaoApiConstants.SORT_ACCURACY)
                .build();
    }

    public static KakaoPlaceSearchRequest keywordWithLocation(String query, BigDecimal longitude,
                                                              BigDecimal latitude, Integer radius) {
        return KakaoPlaceSearchRequest.builder()
                .query(query)
                .longitude(longitude)
                .latitude(latitude)
                .radius(radius)
                .page(KakaoApiConstants.DEFAULT_PAGE)
                .size(15)
                .sort(KakaoApiConstants.SORT_DISTANCE)
                .build();
    }

    public static KakaoPlaceSearchRequest category(String categoryGroupCode, BigDecimal longitude,
                                                   BigDecimal latitude, Integer radius) {
        return KakaoPlaceSearchRequest.builder()
                .categoryGroupCode(categoryGroupCode)
                .longitude(longitude)
                .latitude(latitude)
                .radius(radius)
                .page(KakaoApiConstants.DEFAULT_PAGE)
                .size(15)
                .sort(KakaoApiConstants.SORT_DISTANCE)
                .build();
    }

    public KakaoPlaceSearchRequest withPaging(Integer page, Integer size) {
        this.page = page;
        this.size = size;
        return this;
    }

    public boolean hasLocation() {
        return longitude != null && latitude != null;
    }

    public boolean hasRadius() {
        return radius != null && radius > 0;
    }

}
