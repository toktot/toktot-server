package com.toktot.external.tourapi;

import com.toktot.external.tourapi.config.TourApiProperties;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.dto.TourApiDetailCommon;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.exception.TourApiException;
import com.toktot.external.tourapi.exception.TourApiRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class TourApiClient {

    private final WebClient webClient;
    private final TourApiProperties properties;
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);

    public TourApiClient(@Qualifier("tourApiWebClient") WebClient webClient, TourApiProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public TourApiResponse<TourApiItemsWrapper> getAllJejuRestaurants(int pageNo) {
        checkRateLimit();

        log.info(TourApiConstants.LOG_API_CALL_START,
                TourApiConstants.ENDPOINT_AREA_BASED_LIST, pageNo);

        long startTime = System.currentTimeMillis();

        try {
            TourApiResponse<TourApiItemsWrapper> response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TourApiConstants.ENDPOINT_AREA_BASED_LIST)
                            .queryParam(TourApiConstants.PARAM_SERVICE_KEY, properties.getServiceKey())
                            .queryParam(TourApiConstants.PARAM_NUM_OF_ROWS, properties.getMaxPageSize())
                            .queryParam(TourApiConstants.PARAM_PAGE_NO, pageNo)
                            .queryParam(TourApiConstants.PARAM_MOBILE_OS, properties.getMobileOs())
                            .queryParam(TourApiConstants.PARAM_MOBILE_APP, properties.getMobileApp())
                            .queryParam(TourApiConstants.PARAM_TYPE, properties.getResponseType())
                            .queryParam(TourApiConstants.PARAM_LIST_YN, properties.getListYn())
                            .queryParam(TourApiConstants.PARAM_ARRANGE, properties.getArrange())
                            .queryParam(TourApiConstants.PARAM_CONTENT_TYPE_ID, properties.getRestaurantContentTypeId())
                            .queryParam(TourApiConstants.PARAM_AREA_CODE, properties.getJejuAreaCode())
                            .queryParam(TourApiConstants.PARAM_CAT1, TourApiConstants.CAT1_FOOD)
                            .build())
                    .retrieve()
                    .bodyToMono(TourApiResponse.class)
                    .timeout(Duration.ofMillis(properties.getReadTimeout()))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            dailyCallCount.incrementAndGet();

            if (response != null && isSuccessResponse(response)) {
                log.info(TourApiConstants.LOG_API_CALL_SUCCESS,
                        TourApiConstants.ENDPOINT_AREA_BASED_LIST, responseTime,
                        response.response().body().totalCount());

                return response;
            } else {
                String errorMsg = response != null ?
                        response.response().header().resultMsg() : "null response";
                log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                        TourApiConstants.ENDPOINT_AREA_BASED_LIST, "UNKNOWN", errorMsg);

                throw new TourApiException("API 응답 오류: " + errorMsg, "UNKNOWN", 500);
            }

        } catch (WebClientResponseException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                    TourApiConstants.ENDPOINT_AREA_BASED_LIST, "HTTP_ERROR", e.getMessage());

            throw new TourApiException("HTTP 오류: " + e.getMessage(), "HTTP_ERROR", e.getStatusCode().value());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                    TourApiConstants.ENDPOINT_AREA_BASED_LIST, "EXCEPTION", e.getMessage());

            throw new TourApiException("API 호출 중 예외 발생: " + e.getMessage(), "EXCEPTION", 500);
        }
    }

    public TourApiDetailCommon getRestaurantDetail(String contentId) {
        checkRateLimit();

        log.info(TourApiConstants.LOG_API_CALL_START,
                TourApiConstants.ENDPOINT_DETAIL_COMMON, contentId);

        long startTime = System.currentTimeMillis();

        try {
            TourApiDetailCommon response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(TourApiConstants.ENDPOINT_DETAIL_COMMON)
                            .queryParam(TourApiConstants.PARAM_SERVICE_KEY, properties.getServiceKey())
                            .queryParam(TourApiConstants.PARAM_CONTENT_ID, contentId)
                            .queryParam(TourApiConstants.PARAM_MOBILE_OS, properties.getMobileOs())
                            .queryParam(TourApiConstants.PARAM_MOBILE_APP, properties.getMobileApp())
                            .queryParam(TourApiConstants.PARAM_TYPE, properties.getResponseType())
                            .queryParam(TourApiConstants.PARAM_DEFAULT_YN, TourApiConstants.VALUE_DEFAULT_YN)
                            .queryParam(TourApiConstants.PARAM_FIRST_IMAGE_YN, TourApiConstants.VALUE_FIRST_IMAGE_YN)
                            .queryParam(TourApiConstants.PARAM_ADDR_INFO_YN, TourApiConstants.VALUE_ADDR_INFO_YN)
                            .queryParam(TourApiConstants.PARAM_MAP_INFO_YN, TourApiConstants.VALUE_MAP_INFO_YN)
                            .queryParam(TourApiConstants.PARAM_OVERVIEW_YN, TourApiConstants.VALUE_OVERVIEW_YN)
                            .build())
                    .retrieve()
                    .bodyToMono(TourApiDetailCommon.class)
                    .timeout(Duration.ofMillis(properties.getReadTimeout()))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            dailyCallCount.incrementAndGet();

            log.info(TourApiConstants.LOG_API_CALL_SUCCESS,
                    TourApiConstants.ENDPOINT_DETAIL_COMMON, responseTime, 1);

            return response;

        } catch (WebClientResponseException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                    TourApiConstants.ENDPOINT_DETAIL_COMMON, "HTTP_ERROR", e.getMessage());

            throw new TourApiException("상세정보 HTTP 오류: " + e.getMessage(), "HTTP_ERROR", e.getStatusCode().value());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                    TourApiConstants.ENDPOINT_DETAIL_COMMON, "EXCEPTION", e.getMessage());

            throw new TourApiException("상세정보 조회 실패: " + e.getMessage(), "EXCEPTION", 500);
        }
    }

    public TourApiResponse<TourApiItemsWrapper> getRestaurantsByLocation(double lat, double lng) {
        return getAllJejuRestaurants(1);
    }

    private void checkRateLimit() {
        int currentCount = dailyCallCount.get();
        if (currentCount >= properties.getDailyCallLimit()) {
            log.warn(TourApiConstants.LOG_RATE_LIMIT_WARNING,
                    currentCount, properties.getDailyCallLimit(), 0);

            throw new TourApiRateLimitException(
                    "일일 API 호출 제한 도달", 0, null);
        }

        if (currentCount >= properties.getDailyCallLimit() * 0.9) {
            log.warn(TourApiConstants.LOG_RATE_LIMIT_WARNING,
                    currentCount, properties.getDailyCallLimit(),
                    properties.getDailyCallLimit() - currentCount);
        }
    }

    private boolean isSuccessResponse(TourApiResponse<TourApiItemsWrapper> response) {
        return response.response() != null &&
                response.response().header() != null &&
                TourApiConstants.RESPONSE_CODE_SUCCESS.equals(
                        response.response().header().resultCode());
    }

    public int getCurrentCallCount() {
        return dailyCallCount.get();
    }

    public void resetCallCount() {
        dailyCallCount.set(0);
    }
}
