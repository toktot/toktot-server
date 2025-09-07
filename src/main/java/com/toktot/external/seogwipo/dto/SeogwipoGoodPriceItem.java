package com.toktot.external.seogwipo.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class SeogwipoGoodPriceItem {

    @JacksonXmlProperty(localName = "shop_code")
    private String shopCode;

    @JacksonXmlProperty(localName = "shop_title")
    private String shopTitle;

    @JacksonXmlProperty(localName = "shop_tel")
    private String shopTel;

    @JacksonXmlProperty(localName = "shop_local")
    private String shopLocal;

    @JacksonXmlProperty(localName = "shop_address")
    private String shopAddress;

    @JacksonXmlProperty(localName = "shop_goods")
    private String shopGoods;

    @JacksonXmlProperty(localName = "shop_holiday")
    private String shopHoliday;

    @JacksonXmlProperty(localName = "shop_thumb")
    private String shopThumb;

    public boolean hasValidShopTitle() {
        return shopTitle != null && !shopTitle.trim().isEmpty();
    }

    public boolean hasMenuInfo() {
        return shopGoods != null && !shopGoods.trim().isEmpty();
    }
}
