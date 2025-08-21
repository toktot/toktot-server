package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.external.kakao.KakaoApiConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
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

    public boolean hasValidCoordinates() {
        return getLongitude() != null && getLatitude() != null;
    }

    public boolean isFoodCategory() {
        return categoryGroupCode != null &&
                (categoryGroupCode.equals(KakaoApiConstants.CATEGORY_FOOD) || categoryGroupCode.equals(KakaoApiConstants.CATEGORY_CAFE));
    }

    public String getMainCategory() {
        if (categoryName == null) return null;
        String[] categories = categoryName.split(" > ");
        return categories.length > 0 ? categories[0].trim() : null;
    }

}
