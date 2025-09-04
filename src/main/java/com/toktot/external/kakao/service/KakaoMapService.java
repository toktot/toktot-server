package com.toktot.external.kakao.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.search.type.SortType;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.KakaoApiProperties;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.web.dto.request.LocationFilterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapService {

    @Qualifier("kakaoRestTemplate")
    private final RestTemplate restTemplate;

    private final KakaoApiProperties kakaoApiProperties;

    public KakaoPlaceSearchResponse searchJejuAllFoodAndCafePlace(String query, Integer page, LocationFilterRequest location, SortType sort) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(kakaoApiProperties.getBaseUrl())
                .path(KakaoApiConstants.KEYWORD_ENDPOINT)
                .queryParam(KakaoApiConstants.PARAM_QUERY, query)
                .queryParam(KakaoApiConstants.PARAM_PAGE, page)
                .queryParam(KakaoApiConstants.PARAM_SIZE, KakaoApiConstants.DEFAULT_SIZE)
                .queryParam(KakaoApiConstants.PARAM_RECT, KakaoApiConstants.JEJU_RECT);

        if (location != null) {
            builder.queryParam(KakaoApiConstants.PARAM_LONGITUDE, location.longitude())
                    .queryParam(KakaoApiConstants.PARAM_LATITUDE, location.latitude())
                    .queryParam(KakaoApiConstants.PARAM_RADIUS, location.radius());
        }

        if (sort != null && location != null) {
            builder.queryParam(KakaoApiConstants.PARAM_SORT, sort.getSortForKakao());
        }

        String url = builder.build().toString();

        KakaoPlaceSearchResponse response = executeKakaoPlaceSearch(url);
        return response.filterFoodAndCafe();
    }

    public KakaoPlaceSearchResponse searchRestaurantAddress(String address) {
        String url = UriComponentsBuilder
                .fromUriString(kakaoApiProperties.getBaseUrl())
                .path(KakaoApiConstants.KEYWORD_ENDPOINT)
                .queryParam(KakaoApiConstants.PARAM_QUERY, address)
                .build()
                .toString();

        KakaoPlaceSearchResponse response = executeKakaoPlaceSearch(url);
        return response.filterFoodAndCafe();
    }

    private KakaoPlaceSearchResponse executeKakaoPlaceSearch(String url) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<KakaoPlaceSearchResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            KakaoPlaceSearchResponse response = responseEntity.getBody();

            if (response == null) {
                throw new ToktotException(ErrorCode.KAKAO_LOCAL_SERVICE_ERROR, "Kakao API response body is null.");
            }

            log.info(KakaoApiConstants.LOG_API_CALL_SUCCESS, response.getResultCount());
            return response;
        } catch (RestClientException e) {
            log.error("Failed to call Kakao API. URL: {}", url, e);
            throw new ToktotException(ErrorCode.KAKAO_LOCAL_SERVICE_ERROR, e.getMessage());
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
