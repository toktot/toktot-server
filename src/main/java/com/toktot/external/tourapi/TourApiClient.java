package com.toktot.external.tourapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.external.tourapi.config.TourApiProperties;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.exception.TourApiException;
import com.toktot.external.tourapi.exception.TourApiRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class TourApiClient {

    private final TourApiProperties properties;
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TourApiClient(TourApiProperties properties) {
        this.properties = properties;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public TourApiResponse<TourApiItemsWrapper> getAllJejuRestaurants(int pageNo) {
        checkRateLimit();
        log.info(TourApiConstants.LOG_API_CALL_START, TourApiConstants.ENDPOINT_AREA_BASED_LIST, pageNo);

        long startTime = System.currentTimeMillis();

        try {
            String encodedKey = URLEncoder.encode(properties.getServiceKey(), StandardCharsets.UTF_8);
            String url = buildRestaurantListUrl(encodedKey, pageNo);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(properties.getReadTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            long responseTime = System.currentTimeMillis() - startTime;
            dailyCallCount.incrementAndGet();

            TourApiResponse<TourApiItemsWrapper> apiResponse = objectMapper.readValue(
                    response.body(),
                    objectMapper.getTypeFactory().constructParametricType(
                            TourApiResponse.class, TourApiItemsWrapper.class));

            if (apiResponse != null && isSuccessResponse(apiResponse)) {
                log.info(TourApiConstants.LOG_API_CALL_SUCCESS,
                        TourApiConstants.ENDPOINT_AREA_BASED_LIST, responseTime,
                        apiResponse.response().body().totalCount());
                return apiResponse;
            } else {
                String errorMsg = apiResponse != null ?
                        apiResponse.response().header().resultMsg() : "null response";
                log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                        TourApiConstants.ENDPOINT_AREA_BASED_LIST, "UNKNOWN", errorMsg);
                throw new TourApiException("API 응답 오류: " + errorMsg, "UNKNOWN", 500);
            }

        } catch (Exception e) {
            log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                    TourApiConstants.ENDPOINT_AREA_BASED_LIST, "EXCEPTION", e.getMessage());
            throw new TourApiException("API 호출 중 예외 발생: " + e.getMessage(), "EXCEPTION", 500);
        }
    }

    private String buildRestaurantListUrl(String encodedKey, int pageNo) {
        return properties.getBaseUrl() + TourApiConstants.ENDPOINT_AREA_BASED_LIST +
                "?serviceKey=" + encodedKey +
                "&numOfRows=" + properties.getMaxPageSize() +
                "&pageNo=" + pageNo +
                "&MobileOS=" + properties.getMobileOs() +
                "&MobileApp=" + properties.getMobileApp() +
                "&_type=" + properties.getResponseType() +
                "&contentTypeId=" + properties.getRestaurantContentTypeId() +
                "&areaCode=" + properties.getJejuAreaCode() +
                "&cat1=" + TourApiConstants.CAT1_FOOD;
    }

    private void checkRateLimit() {
        int currentCount = dailyCallCount.get();
        if (currentCount >= properties.getDailyCallLimit()) {
            log.warn(TourApiConstants.LOG_RATE_LIMIT_WARNING,
                    currentCount, properties.getDailyCallLimit(), 0);
            throw new TourApiRateLimitException("일일 API 호출 제한 도달", 0, null);
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

}
