package com.toktot.external.kakao;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.kakao.dto.response.KakaoPlace;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public final class KakaoApiConstants {

    private KakaoApiConstants() {}

    public static final String SEARCH_ENDPOINT = "/v2/local/search/keyword.json";
    public static final String CATEGORY_ENDPOINT = "/v2/local/search/category.json";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_AUTH_PREFIX = "KakaoAK ";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    public static final String PARAM_QUERY = "query";
    public static final String PARAM_CATEGORY_GROUP_CODE = "category_group_code";
    public static final String PARAM_X = "x";
    public static final String PARAM_Y = "y";
    public static final String PARAM_RADIUS = "radius";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_SORT = "sort";

    public static final String CATEGORY_FOOD = "FD6";
    public static final String CATEGORY_CAFE = "CE7";

    public static final String SORT_DISTANCE = "distance";
    public static final String SORT_ACCURACY = "accuracy";

    public static final String ERROR_INVALID_API_KEY = "InvalidApiKeyException";
    public static final String ERROR_QUOTA_EXCEEDED = "QuotaExceededException";
    public static final String ERROR_NETWORK = "NetworkException";

    public static final String LOG_API_CALL_START = "카카오맵 API 호출 시작: endpoint={}, query={}";
    public static final String LOG_API_CALL_SUCCESS = "카카오맵 API 호출 성공: responseTime={}ms, resultCount={}";
    public static final String LOG_API_CALL_FAILURE = "카카오맵 API 호출 실패: endpoint={}, errorMessage={}";

    public static final int DEFAULT_PAGE = 1;
    public static final int MIN_RADIUS = 0;
    public static final int MAX_RADIUS = 20000;

}
