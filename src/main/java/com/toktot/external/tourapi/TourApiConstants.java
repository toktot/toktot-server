package com.toktot.external.tourapi;

public final class TourApiConstants {

    private TourApiConstants() {
    }

    public static final String ENDPOINT_AREA_BASED_LIST = "/areaBasedList1";
    public static final String ENDPOINT_DETAIL_COMMON = "/detailCommon1";
    public static final String ENDPOINT_DETAIL_INTRO = "/detailIntro1";
    public static final String ENDPOINT_DETAIL_IMAGE = "/detailImage1";

    public static final String PARAM_SERVICE_KEY = "serviceKey";
    public static final String PARAM_NUM_OF_ROWS = "numOfRows";
    public static final String PARAM_PAGE_NO = "pageNo";
    public static final String PARAM_MOBILE_OS = "MobileOS";
    public static final String PARAM_MOBILE_APP = "MobileApp";
    public static final String PARAM_TYPE = "_type";
    public static final String PARAM_LIST_YN = "listYN";
    public static final String PARAM_ARRANGE = "arrange";
    public static final String PARAM_CONTENT_TYPE_ID = "contentTypeId";
    public static final String PARAM_AREA_CODE = "areaCode";
    public static final String PARAM_SIGUNGU_CODE = "sigunguCode";
    public static final String PARAM_CAT1 = "cat1";
    public static final String PARAM_CAT2 = "cat2";
    public static final String PARAM_CAT3 = "cat3";
    public static final String PARAM_CONTENT_ID = "contentId";
    public static final String PARAM_DEFAULT_YN = "defaultYN";
    public static final String PARAM_FIRST_IMAGE_YN = "firstImageYN";
    public static final String PARAM_ADDR_INFO_YN = "addrinfoYN";
    public static final String PARAM_MAP_INFO_YN = "mapinfoYN";
    public static final String PARAM_OVERVIEW_YN = "overviewYN";

    public static final String VALUE_MOBILE_OS = "ETC";
    public static final String VALUE_MOBILE_APP = "TOKTOT";
    public static final String VALUE_TYPE_JSON = "json";
    public static final String VALUE_LIST_YN = "Y";
    public static final String VALUE_DEFAULT_YN = "Y";
    public static final String VALUE_FIRST_IMAGE_YN = "Y";
    public static final String VALUE_ADDR_INFO_YN = "Y";
    public static final String VALUE_MAP_INFO_YN = "Y";
    public static final String VALUE_OVERVIEW_YN = "Y";

    public static final String AREA_CODE_JEJU = "39";
    public static final String SIGUNGU_CODE_JEJU_CITY = "1";
    public static final String SIGUNGU_CODE_SEOGWIPO = "2";
    public static final String CONTENT_TYPE_RESTAURANT = "39";
    public static final String CONTENT_TYPE_CAFE = "39";

    public static final String CAT1_FOOD = "A05";
    public static final String CAT2_RESTAURANT = "A0502";
    public static final String CAT2_CAFE = "A0503";

    public static final String ARRANGE_TITLE = "A";
    public static final String ARRANGE_VIEW = "B";
    public static final String ARRANGE_MODIFIED = "C";
    public static final String ARRANGE_CREATED = "D";

    public static final String RESPONSE_CODE_SUCCESS = "0000";
    public static final String RESPONSE_CODE_AUTH_ERROR = "1001";
    public static final String RESPONSE_CODE_SERVICE_SUSPENDED = "3001";
    public static final String RESPONSE_CODE_TRAFFIC_EXCEEDED = "3002";
    public static final String RESPONSE_CODE_SERVICE_NOT_REGISTERED = "3003";

    public static final String ERROR_MSG_INVALID_SERVICE_KEY = "잘못된 서비스키입니다.";
    public static final String ERROR_MSG_TRAFFIC_EXCEEDED = "일일 트래픽이 초과되었습니다.";
    public static final String ERROR_MSG_SERVICE_SUSPENDED = "서비스가 일시 중단되었습니다.";
    public static final String ERROR_MSG_NETWORK_ERROR = "네트워크 오류가 발생했습니다.";
    public static final String ERROR_MSG_PARSING_ERROR = "응답 데이터 파싱 중 오류가 발생했습니다.";
    public static final String ERROR_MSG_TIMEOUT = "API 호출 시간이 초과되었습니다.";
    public static final String ERROR_MSG_RATE_LIMIT = "API 호출 제한에 도달했습니다.";

    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 1000;
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int MAX_RETRY_COUNT = 3;
    public static final int DEFAULT_TIMEOUT_MS = 10000;

    public static final String LOG_API_CALL_START = "TourAPI 호출 시작: endpoint={}, pageNo={}";
    public static final String LOG_API_CALL_SUCCESS = "TourAPI 호출 성공: endpoint={}, responseTime={}ms, resultCount={}";
    public static final String LOG_API_CALL_FAILURE = "TourAPI 호출 실패: endpoint={}, errorCode={}, errorMessage={}";
    public static final String LOG_RATE_LIMIT_WARNING = "TourAPI 호출 제한 경고: 현재 호출 수={}, 제한={}, 잔여={}";
    public static final String LOG_RETRY_ATTEMPT = "TourAPI 재시도 시도: attempt={}/{}, endpoint={}";

    public static final String METRIC_API_CALL_COUNT = "tourapi.call.count";
    public static final String METRIC_API_CALL_DURATION = "tourapi.call.duration";
    public static final String METRIC_API_CALL_ERROR = "tourapi.call.error";
    public static final String METRIC_API_RATE_LIMIT = "tourapi.rate.limit";
}
