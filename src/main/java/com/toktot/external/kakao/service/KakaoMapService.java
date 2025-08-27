package com.toktot.external.kakao.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.KakaoApiProperties;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapService {


    @Qualifier("kakaoRestTemplate")
    private final RestTemplate restTemplate;

    private final KakaoApiProperties kakaoApiProperties;

    public KakaoPlaceSearchResponse searchJejuAllFoodAndCafePlace(String query, Integer page) {
        String endpoint = kakaoApiProperties.getBaseUrl() + KakaoApiConstants.KEYWORD_ENDPOINT;

        try {
            String url = UriComponentsBuilder
                    .fromUriString(kakaoApiProperties.getBaseUrl())
                    .path(KakaoApiConstants.KEYWORD_ENDPOINT)
                    .queryParam(KakaoApiConstants.PARAM_QUERY, query)
                    .queryParam(KakaoApiConstants.PARAM_PAGE, page)
                    .queryParam(KakaoApiConstants.PARAM_SIZE, KakaoApiConstants.DEFAULT_SIZE)
                    .queryParam(KakaoApiConstants.PARAM_RECT, KakaoApiConstants.JEJU_RECT)
                    .build()
                    .toString();

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<KakaoPlaceSearchResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            KakaoPlaceSearchResponse response = responseEntity.getBody();

            log.info(
                    KakaoApiConstants.LOG_API_CALL_SUCCESS,
                    response != null ? response.getResultCount() : 0);
            return response.filterFoodAndCafe();
        } catch (Exception e) {
            throw new ToktotException(ErrorCode.KAKAO_LOCAL_SERVICE_ERROR, e.getMessage() + endpoint);
        }
    }

    public KakaoPlaceSearchResponse searchRestaurantByNameAndCoordinates(String placeName, BigDecimal longitude, BigDecimal latitude) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kakaoApiProperties.getBaseUrl())
                    .path(KakaoApiConstants.KEYWORD_ENDPOINT)
                    .queryParam(KakaoApiConstants.PARAM_QUERY, placeName)
                    .queryParam(KakaoApiConstants.PARAM_LONGITUDE, longitude.toString())
                    .queryParam(KakaoApiConstants.PARAM_LATITUDE, latitude.toString())
                    .queryParam(KakaoApiConstants.PARAM_RADIUS, 10)
                    .build()
                    .toString();

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<KakaoPlaceSearchResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            KakaoPlaceSearchResponse response = responseEntity.getBody();

            log.info(
                    KakaoApiConstants.LOG_API_CALL_SUCCESS,
                    response != null ? response.getResultCount() : 0);
            return response.filterFoodAndCafe();
        } catch (Exception e) {
            throw new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND);
        }
    }

    public KakaoPlaceSearchResponse searchRestaurantAddress(String address) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(kakaoApiProperties.getBaseUrl())
                    .path(KakaoApiConstants.KEYWORD_ENDPOINT)
                    .queryParam(KakaoApiConstants.PARAM_QUERY, address)
                    .build()
                    .toString();

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<KakaoPlaceSearchResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            KakaoPlaceSearchResponse response = responseEntity.getBody();

            log.info(
                    KakaoApiConstants.LOG_API_CALL_SUCCESS,
                    response != null ? response.getResultCount() : 0);
            return response.filterFoodAndCafe();
        } catch (Exception e) {
            throw new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(KakaoApiConstants.HEADER_AUTHORIZATION,
                KakaoApiConstants.HEADER_AUTH_PREFIX + kakaoApiProperties.getApiKey());
        headers.set(KakaoApiConstants.HEADER_CONTENT_TYPE, KakaoApiConstants.CONTENT_TYPE_JSON);
        return headers;
    }

}
