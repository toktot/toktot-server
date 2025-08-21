package com.toktot.external.kakao.dto.request;

import com.toktot.external.kakao.KakaoApiConstants;

import java.math.BigDecimal;

public record KakaoPlaceSearchRequest(
        String query,
        BigDecimal longitude,
        BigDecimal latitude,
        String rect,
        Integer radius,
        Integer page,
        String sort
) {

    public KakaoPlaceSearchRequest plusPage() {
        return new KakaoPlaceSearchRequest(query, longitude, latitude, rect, radius, page + 1, sort);
    }

    public static KakaoPlaceSearchRequest keyword(String query) {
        return new KakaoPlaceSearchRequest(
                query,
                null,
                null,
                null,
                null,
                KakaoApiConstants.DEFAULT_PAGE,
                KakaoApiConstants.SORT_ACCURACY
        );
    }

    public static KakaoPlaceSearchRequest keywordWithLocation(String query, BigDecimal longitude,
                                                              BigDecimal latitude, Integer radius) {
        return new KakaoPlaceSearchRequest(
                query,
                longitude,
                latitude,
                null,
                radius,
                KakaoApiConstants.DEFAULT_PAGE,
                KakaoApiConstants.SORT_DISTANCE
        );
    }

    public static KakaoPlaceSearchRequest category(String categoryGroupCode, BigDecimal longitude,
                                                   BigDecimal latitude, Integer radius) {
        return new KakaoPlaceSearchRequest(
                null,
                longitude,
                latitude,
                null,
                radius,
                KakaoApiConstants.DEFAULT_PAGE,
                KakaoApiConstants.SORT_DISTANCE
        );
    }

    public KakaoPlaceSearchRequest withPaging(Integer page, Integer size) {
        return new KakaoPlaceSearchRequest(
                this.query,
                this.longitude,
                this.latitude,
                this.rect,
                this.radius,
                page,
                this.sort
        );
    }

    public boolean hasLocation() {
        return longitude != null && latitude != null;
    }

    public boolean hasRadius() {
        return radius != null && radius > 0;
    }
}
