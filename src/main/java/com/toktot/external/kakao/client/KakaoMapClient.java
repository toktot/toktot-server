package com.toktot.external.kakao.client;

import com.toktot.external.kakao.KakaoApiProperties;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.request.KakaoPlaceSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoApiResponse;
import com.toktot.external.kakao.dto.response.KakaoPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoMapClient {

    @Qualifier("kakaoRestTemplate")
    private final RestTemplate restTemplate;

    private final KakaoApiProperties kakaoApiProperties;

    public KakaoApiResponse<KakaoPlace> searchPlaces(KakaoPlaceSearchRequest request) {
        long startTime = System.currentTimeMillis();
        String endpoint = kakaoApiProperties.getBaseUrl() + KakaoApiConstants.SEARCH_ENDPOINT;

        try {
            log.info(KakaoApiConstants.LOG_API_CALL_START, endpoint, request.getQuery());

            String url = buildSearchUrl(request);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoApiResponse<KakaoPlace>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            KakaoApiResponse<KakaoPlace> result = response.getBody();
            long responseTime = System.currentTimeMillis() - startTime;

            log.info(KakaoApiConstants.LOG_API_CALL_SUCCESS, responseTime,
                    result != null ? result.getResultCount() : 0);

            return result;

        } catch (Exception e) {
            log.error(KakaoApiConstants.LOG_API_CALL_FAILURE, endpoint, e.getMessage());
            throw new RuntimeException("카카오맵 API 호출 실패", e);
        }
    }

    public KakaoApiResponse<KakaoPlace> searchPlacesByCategory(KakaoPlaceSearchRequest request) {
        long startTime = System.currentTimeMillis();
        String endpoint = kakaoApiProperties.getBaseUrl() + KakaoApiConstants.CATEGORY_ENDPOINT;

        try {
            log.info(KakaoApiConstants.LOG_API_CALL_START, endpoint, request.getCategoryGroupCode());

            String url = buildCategoryUrl(request);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoApiResponse<KakaoPlace>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            KakaoApiResponse<KakaoPlace> result = response.getBody();
            long responseTime = System.currentTimeMillis() - startTime;

            log.info(KakaoApiConstants.LOG_API_CALL_SUCCESS, responseTime,
                    result != null ? result.getResultCount() : 0);

            return result;

        } catch (Exception e) {
            log.error(KakaoApiConstants.LOG_API_CALL_FAILURE, endpoint, e.getMessage());
            throw new RuntimeException("카카오맵 카테고리 검색 실패", e);
        }
    }

    private String buildSearchUrl(KakaoPlaceSearchRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(kakaoApiProperties.getBaseUrl() + KakaoApiConstants.SEARCH_ENDPOINT)
                .queryParam(KakaoApiConstants.PARAM_QUERY, request.getQuery())
                .queryParam(KakaoApiConstants.PARAM_PAGE, request.getPage())
                .queryParam(KakaoApiConstants.PARAM_SIZE, request.getSize())
                .queryParam(KakaoApiConstants.PARAM_SORT, request.getSort());

        if (request.hasLocation()) {
            builder.queryParam(KakaoApiConstants.PARAM_X, request.getLongitude())
                    .queryParam(KakaoApiConstants.PARAM_Y, request.getLatitude());
        }

        if (request.hasRadius()) {
            builder.queryParam(KakaoApiConstants.PARAM_RADIUS, request.getRadius());
        }

        return builder.build().toString();
    }

    private String buildCategoryUrl(KakaoPlaceSearchRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(kakaoApiProperties.getBaseUrl() + KakaoApiConstants.CATEGORY_ENDPOINT)
                .queryParam(KakaoApiConstants.PARAM_CATEGORY_GROUP_CODE, request.getCategoryGroupCode())
                .queryParam(KakaoApiConstants.PARAM_PAGE, request.getPage())
                .queryParam(KakaoApiConstants.PARAM_SIZE, request.getSize())
                .queryParam(KakaoApiConstants.PARAM_SORT, request.getSort());

        if (request.hasLocation()) {
            builder.queryParam(KakaoApiConstants.PARAM_X, request.getLongitude())
                    .queryParam(KakaoApiConstants.PARAM_Y, request.getLatitude());
        }

        if (request.hasRadius()) {
            builder.queryParam(KakaoApiConstants.PARAM_RADIUS, request.getRadius());
        }

        return builder.build().toString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(KakaoApiConstants.HEADER_AUTHORIZATION,
                KakaoApiConstants.HEADER_AUTH_PREFIX + kakaoApiProperties.getApiKey());
        headers.set(KakaoApiConstants.HEADER_CONTENT_TYPE, KakaoApiConstants.CONTENT_TYPE_JSON);
        return headers;
    }
}
