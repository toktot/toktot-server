package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoPlaceInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("place_name")
    private String placeName;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("category_group_code")
    private String categoryGroupCode;

    @JsonProperty("category_group_name")
    private String categoryGroupName;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("address_name")
    private String addressName;

    @JsonProperty("road_address_name")
    private String roadAddressName;

    @JsonProperty("x")
    private String x;

    @JsonProperty("y")
    private String y;

    @JsonProperty("place_url")
    private String placeUrl;

    @JsonProperty("distance")
    private String distance;

    public BigDecimal getLongitude() {
        try {
            return x != null ? new BigDecimal(x) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public BigDecimal getLatitude() {
        try {
            return y != null ? new BigDecimal(y) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Restaurant toEntity() {
        return Restaurant.builder()
                .externalKakaoId(id)
                .name(placeName)
                .category(categoryGroupName)
                .address(roadAddressName)
                .latitude(getLatitude())
                .longitude(getLongitude())
                .phone(phone)
                .dataSource(DataSource.KAKAO)
                .website(placeUrl)
                .build();
    }

}
