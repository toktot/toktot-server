package com.toktot.external.kakao.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.KakaoApiProperties;
import com.toktot.external.kakao.dto.request.RestaurantSearchRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapService {


    @Qualifier("kakaoRestTemplate")
    private final RestTemplate restTemplate;

    private final KakaoApiProperties kakaoApiProperties;

    public KakaoPlaceSearchResponse searchJejuAllFoodAndCafePlace(RestaurantSearchRequest request) {
        String endpoint = kakaoApiProperties.getBaseUrl() + KakaoApiConstants.KEYWORD_ENDPOINT;

        try {
            String url = UriComponentsBuilder
                    .fromUriString(kakaoApiProperties.getBaseUrl())
                    .path(KakaoApiConstants.KEYWORD_ENDPOINT)
                    .queryParam(KakaoApiConstants.PARAM_QUERY, request.query())
                    .queryParam(KakaoApiConstants.PARAM_PAGE, request.page())
                    .queryParam(KakaoApiConstants.PARAM_SIZE, KakaoApiConstants.DEFAULT_SIZE)
                    .queryParam(KakaoApiConstants.PARAM_RECT, request.rect())
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

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(KakaoApiConstants.HEADER_AUTHORIZATION,
                KakaoApiConstants.HEADER_AUTH_PREFIX + kakaoApiProperties.getApiKey());
        headers.set(KakaoApiConstants.HEADER_CONTENT_TYPE, KakaoApiConstants.CONTENT_TYPE_JSON);
        return headers;
    }

}
