package com.toktot.external.seogwipo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SeogwipoGoodPriceItem {

    @JsonProperty("경도")
    private String longitude;

    @JsonProperty("데이터기준일자")
    private String dataDate;

    @JsonProperty("업소명")
    private String shopTitle;

    @JsonProperty("업종")
    private String businessType;

    @JsonProperty("연락처")
    private String shopTel;

    @JsonProperty("위도")
    private String latitude;

    @JsonProperty("주소")
    private String shopAddress;

    public boolean hasValidShopTitle() {
        return shopTitle != null && !shopTitle.trim().isEmpty();
    }

    public boolean isRestaurant() {
        if (businessType == null) {
            return false;
        }

        return businessType.contains("한식")
                || businessType.contains("중식")
                || businessType.contains("일식")
                || businessType.contains("양식")
                || businessType.contains("분식")
                || businessType.contains("카페")
                || businessType.contains("식당")
                || businessType.contains("음식점")
                || businessType.contains("뷔페")
                || businessType.contains("레스토랑");
    }

    public boolean hasMenuInfo() {
        return businessType != null && !businessType.trim().isEmpty() && isRestaurant();
    }

    public String getShopGoods() {
        return businessType;
    }

    public String getShopCode() {
        return null;
    }

}
