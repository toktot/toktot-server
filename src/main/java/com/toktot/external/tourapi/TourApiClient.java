package com.toktot.external.tourapi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.external.tourapi.config.TourApiProperties;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.exception.TourApiRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                return processSuccessResponse(response.body(), responseTime, pageNo);
            } else {
                log.error("TourAPI HTTP 오류: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("API 호출 실패: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            log.error(TourApiConstants.LOG_API_CALL_FAILURE,
                    TourApiConstants.ENDPOINT_AREA_BASED_LIST, "EXCEPTION", e.getMessage());
            throw new RuntimeException("API 호출 중 예외 발생: " + e.getMessage(), e);
        }
    }

    private TourApiResponse<TourApiItemsWrapper> processSuccessResponse(String responseBody, long responseTime, int pageNo) {
        try {
            log.debug("TourAPI Raw Response: {}", responseBody);

            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("TourAPI 빈 응답 본문");
                return null;
            }

            if (responseBody.contains("\"resultCode\"") &&
                    responseBody.contains("\"resultMsg\"") &&
                    !responseBody.contains("\"response\"")) {

                log.error("TourAPI 오류 응답: {}", responseBody);
                handleErrorResponse(responseBody);
                return null;
            }

            TourApiResponse<TourApiItemsWrapper> apiResponse = objectMapper.readValue(
                    responseBody,
                    objectMapper.getTypeFactory().constructParametricType(
                            TourApiResponse.class,
                            TourApiItemsWrapper.class
                    )
            );

            return validateAndProcessResponse(apiResponse, responseTime);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            if (e.getMessage().contains("Cannot coerce empty String")) {
                log.info("TourAPI 마지막 페이지 도달 (빈 items): pageNo={}", pageNo);
                return createEmptyResponse();
            } else {
                log.error("TourAPI JSON 파싱 오류: {}. 원본 응답: {}", e.getMessage(),
                        responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
                throw new RuntimeException("API 응답 파싱 실패: " + e.getMessage(), e);
            }
        }
    }

    private void handleErrorResponse(String responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode errorNode = objectMapper.readTree(responseBody);
            String errorCode = errorNode.get("resultCode").asText();
            String errorMsg = errorNode.get("resultMsg").asText();

            log.error("TourAPI 오류 - Code: {}, Message: {}", errorCode, errorMsg);
            throw new RuntimeException("TourAPI 오류: " + errorCode + " - " + errorMsg);

        } catch (Exception parseEx) {
            log.error("오류 응답 파싱 실패: {}", parseEx.getMessage());
            throw new RuntimeException("TourAPI 오류 응답: " + responseBody);
        }
    }

    private TourApiResponse<TourApiItemsWrapper> validateAndProcessResponse(
            TourApiResponse<TourApiItemsWrapper> apiResponse, long responseTime) {

        log.debug("파싱된 응답 - apiResponse: {}", apiResponse != null ? "존재" : "null");
        if (apiResponse != null) {
            log.debug("response 필드: {}", apiResponse.response() != null ? "존재" : "null");
            if (apiResponse.response() != null) {
                log.debug("header 필드: {}", apiResponse.response().header() != null ? "존재" : "null");
                log.debug("body 필드: {}", apiResponse.response().body() != null ? "존재" : "null");
            }
        }

        if (apiResponse != null && apiResponse.response() != null && apiResponse.response().header() != null) {
            String resultCode = apiResponse.response().header().resultCode();
            String resultMsg = apiResponse.response().header().resultMsg();

            log.info("TourAPI 응답 헤더 - resultCode: {}, resultMsg: {}", resultCode, resultMsg);

            if (TourApiConstants.RESPONSE_CODE_SUCCESS.equals(resultCode)) {
                int resultCount = 0;
                if (apiResponse.response().body() != null &&
                        apiResponse.response().body().items() != null &&
                        apiResponse.response().body().items().item() != null) {
                    resultCount = apiResponse.response().body().items().item().size();
                }

                log.info(TourApiConstants.LOG_API_CALL_SUCCESS,
                        TourApiConstants.ENDPOINT_AREA_BASED_LIST, responseTime, resultCount);

                return apiResponse;
            } else {
                log.warn("TourAPI 오류 응답: resultCode={}, resultMsg={}", resultCode, resultMsg);
                throw new RuntimeException("TourAPI 오류: " + resultCode + " - " + resultMsg);
            }
        } else {
            log.warn("TourAPI 응답 구조 오류: 필수 필드 누락");
            return null;
        }
    }

    private TourApiResponse<TourApiItemsWrapper> createEmptyResponse() {
        return new TourApiResponse<>(
                new TourApiResponse.TourApiResponseBody<>(
                        new TourApiResponse.TourApiHeader("0000", "OK"),
                        new TourApiResponse.TourApiBody<>(null, 0, 0, 0)
                )
        );
    }

    private String buildRestaurantListUrl(String encodedKey, int pageNo) {
        StringBuilder url = new StringBuilder(properties.getBaseUrl())
                .append(TourApiConstants.ENDPOINT_AREA_BASED_LIST)
                .append("?serviceKey=").append(encodedKey)
                .append("&numOfRows=100")
                .append("&pageNo=").append(pageNo)
                .append("&MobileOS=ETC")
                .append("&MobileApp=TOKTOT")
                .append("&_type=json")
                .append("&areaCode=39")
                .append("&cat1=").append(TourApiConstants.CAT1_FOOD);

        String finalUrl = url.toString();
        log.debug("TourAPI 요청 URL: {}", finalUrl.replaceAll("serviceKey=[^&]*", "serviceKey=***"));

        return finalUrl;
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

}
