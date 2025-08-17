package com.toktot.external.tourapi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TourApiConstants {

    // API Endpoints
    public static final String ENDPOINT_AREA_BASED_LIST = "/areaBasedList2";
    public static final String ENDPOINT_DETAIL_COMMON = "/detailCommon2";
    public static final String ENDPOINT_DETAIL_INTRO = "/detailIntro2";
    public static final String ENDPOINT_DETAIL_IMAGE = "/detailImage2";

    // Category Codes
    public static final String CAT1_FOOD = "A05";

    // Response Codes
    public static final String RESPONSE_CODE_SUCCESS = "0000";

    // Log Messages
    public static final String LOG_API_CALL_START = "TourAPI 호출 시작: endpoint={}, pageNo={}";
    public static final String LOG_API_CALL_SUCCESS = "TourAPI 호출 성공: endpoint={}, responseTime={}ms, resultCount={}";
    public static final String LOG_API_CALL_FAILURE = "TourAPI 호출 실패: endpoint={}, errorCode={}, errorMessage={}";
    public static final String LOG_RATE_LIMIT_WARNING = "TourAPI 호출 제한 경고: 현재 호출 수={}, 제한={}, 잔여={}";

}
