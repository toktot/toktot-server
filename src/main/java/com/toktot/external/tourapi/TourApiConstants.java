package com.toktot.external.tourapi;

public final class TourApiConstants {

    private TourApiConstants() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    // ===== API 엔드포인트 =====
    public static final String ENDPOINT_AREA_BASED_LIST = "/areaBasedList1";
    public static final String ENDPOINT_DETAIL_COMMON = "/detailCommon1";
    public static final String ENDPOINT_DETAIL_INTRO = "/detailIntro1";
    public static final String ENDPOINT_DETAIL_IMAGE = "/detailImage1";

    // ===== 요청 파라미터명 =====
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

    // ===== 고정 파라미터 값 =====
    public static final String MOBILE_OS = "ETC";
    public static final String MOBILE_APP = "TokTot";
    public static final String TYPE_JSON = "json";
    public static final String LIST_YN_Y = "Y";
    public static final String DEFAULT_YN_Y = "Y";
    public static final String FIRST_IMAGE_YN_Y = "Y";
    public static final String ADDR_INFO_YN_Y = "Y";
    public static final String MAP_INFO_YN_Y = "Y";
    public static final String OVERVIEW_YN_Y = "Y";

    // ===== 제주도 지역코드 =====
    public static final String JEJU_AREA_CODE = "39";
    public static final String JEJU_CITY_SIGUNGU_CODE = "1"; // 제주시
    public static final String SEOGWIPO_CITY_SIGUNGU_CODE = "2"; // 서귀포시

    // ===== 컨텐츠 타입 (관광타입) =====
    public static final String CONTENT_TYPE_RESTAURANT = "39"; // 음식점
    public static final String CONTENT_TYPE_TOURIST_SPOT = "12"; // 관광지
    public static final String CONTENT_TYPE_ACCOMMODATION = "32"; // 숙박
    public static final String CONTENT_TYPE_SHOPPING = "38"; // 쇼핑

    // ===== 카테고리 코드 =====
    public static final String CAT1_FOOD = "A05"; // 음식
    public static final String CAT2_KOREAN = "A0502"; // 한식
    public static final String CAT2_WESTERN = "A0503"; // 서양식
    public static final String CAT2_JAPANESE = "A0504"; // 일식
    public static final String CAT2_CHINESE = "A0505"; // 중식
    public static final String CAT2_CAFE = "A0507"; // 카페/전통차

    // ===== 정렬 기준 =====
    public static final String ARRANGE_CREATED_DATE = "C"; // 등록일순
    public static final String ARRANGE_MODIFIED_DATE = "D"; // 수정일순
    public static final String ARRANGE_TITLE = "B"; // 제목순
    public static final String ARRANGE_READCOUNT = "R"; // 조회수순

    // ===== API 응답 결과 코드 =====
    public static final String RESULT_CODE_SUCCESS = "0000";
    public static final String RESULT_CODE_APPLICATION_ERROR = "0001";
    public static final String RESULT_CODE_DB_ERROR = "0002";
    public static final String RESULT_CODE_NODATA_ERROR = "0003";
    public static final String RESULT_CODE_HTTP_ERROR = "0004";
    public static final String RESULT_CODE_SERVICETIME_OUT = "0005";
    public static final String RESULT_CODE_INVALID_REQUEST_PARAMETER_ERROR = "0010";
    public static final String RESULT_CODE_NO_MANDATORY_REQUEST_PARAMETERS_ERROR = "0011";
    public static final String RESULT_CODE_NO_OPENAPI_SERVICE_ERROR = "0012";
    public static final String RESULT_CODE_SERVICE_ACCESS_DENIED_ERROR = "0013";
    public static final String RESULT_CODE_TEMPORARILY_DISABLE_THE_SERVICEKEY_ERROR = "0014";
    public static final String RESULT_CODE_REQUEST_TRAFFIC_LIMIT_EXCEEDED_ERROR = "0015";

    // ===== 기본 설정값 =====
    public static final int DEFAULT_NUM_OF_ROWS = 100; // 페이지당 결과 수
    public static final int MAX_NUM_OF_ROWS = 1000; // 최대 페이지당 결과 수
    public static final int DEFAULT_PAGE_NO = 1; // 기본 페이지 번호

    // ===== 배치 처리 설정 =====
    public static final int BATCH_CHUNK_SIZE = 100; // 배치 처리 청크 크기
    public static final int MAX_RETRY_COUNT = 3; // 최대 재시도 횟수
    public static final long RETRY_DELAY_MS = 1000; // 재시도 지연시간 (밀리초)

    // ===== API 호출 제한 =====
    public static final int DAILY_API_CALL_LIMIT = 1000; // 일일 호출 제한
    public static final int WARNING_THRESHOLD = 900; // 경고 임계값 (90%)
    public static final int DANGER_THRESHOLD = 950; // 위험 임계값 (95%)

    // ===== 타임아웃 설정 =====
    public static final int CONNECTION_TIMEOUT_MS = 5000; // 연결 타임아웃 5초
    public static final int READ_TIMEOUT_MS = 10000; // 읽기 타임아웃 10초
}
