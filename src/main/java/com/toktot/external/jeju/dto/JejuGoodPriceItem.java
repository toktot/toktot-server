package com.toktot.external.jeju.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@NoArgsConstructor
@ToString
public class JejuGoodPriceItem {

    private static final Set<String> ALLOWED_INDUSTRY_TYPES = Set.of(
            "한식", "중식", "일식", "제과", "분식", "기타요식업", "카페", "양식"
    );

    @JsonProperty("dataCd")
    private String dataCd;

    @JsonProperty("regDt")
    private String regDt;

    @JsonProperty("laCrdnt")
    private String laCrdnt;

    @JsonProperty("loCrdnt")
    private String loCrdnt;

    @JsonProperty("slctnYr")
    private String slctnYr;

    @JsonProperty("slctnMm")
    private String slctnMm;

    @JsonProperty("bsshNm")
    private String bsshNm;

    @JsonProperty("indutyNm")
    private String indutyNm;

    @JsonProperty("bsshTelno")
    private String bsshTelno;

    @JsonProperty("prdlstCn")
    private String prdlstCn;

    @JsonProperty("emdNm")
    private String emdNm;

    @JsonProperty("rnAdres")
    private String rnAdres;

    @JsonProperty("etcCn")
    private String etcCn;

    public boolean hasValidBusinessName() {
        return StringUtils.hasText(bsshNm) && !bsshNm.trim().equals("정보없음");
    }

    public boolean hasMenuInfo() {
        return StringUtils.hasText(prdlstCn) && !prdlstCn.trim().equals("정보없음");
    }

    public boolean isValidIndustryType() {
        return StringUtils.hasText(indutyNm) && ALLOWED_INDUSTRY_TYPES.contains(indutyNm.trim());
    }

    public boolean hasValidCoordinates() {
        return isValidLatitude() && isValidLongitude();
    }

    private boolean isValidLatitude() {
        try {
            if (!StringUtils.hasText(laCrdnt)) return false;
            BigDecimal lat = new BigDecimal(laCrdnt);
            return lat.compareTo(new BigDecimal("33.1")) >= 0 &&
                   lat.compareTo(new BigDecimal("33.6")) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidLongitude() {
        try {
            if (!StringUtils.hasText(loCrdnt)) return false;
            BigDecimal lon = new BigDecimal(loCrdnt);
            return lon.compareTo(new BigDecimal("126.1")) >= 0 &&
                   lon.compareTo(new BigDecimal("127.0")) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isValidForProcessing() {
        return hasValidBusinessName() &&
               hasMenuInfo() &&
               isValidIndustryType() &&
               hasValidCoordinates();
    }

    public String getCleanBusinessName() {
        return StringUtils.hasText(bsshNm) ? bsshNm.trim() : null;
    }

    public String getCleanMenuInfo() {
        return StringUtils.hasText(prdlstCn) ? prdlstCn.trim() : null;
    }

    public String getCleanIndustryType() {
        return StringUtils.hasText(indutyNm) ? indutyNm.trim() : null;
    }
}
