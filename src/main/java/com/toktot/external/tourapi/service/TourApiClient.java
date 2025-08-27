package com.toktot.external.tourapi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.tourapi.dto.TourApiConstants;
import com.toktot.external.tourapi.config.TourApiProperties;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.mapper.TourApiDetailIntroWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
public class TourApiClient {

    private final TourApiProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public TourApiClient(TourApiProperties properties) {
        this.properties = properties;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public TourApiResponse<TourApiItemsWrapper> getAllJejuRestaurants() {
        try {
            String encodedKey = URLEncoder.encode(properties.getServiceKey(), StandardCharsets.UTF_8);
            String url = buildRestaurantListUrl(encodedKey);
            HttpResponse<String> response = getStringHttpResponse(url);
            return getTourApiResponse(response);
        } catch (Exception e) {
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, e.getMessage());
        }
    }

    private TourApiResponse<TourApiItemsWrapper> getTourApiResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }

        String body = response.body();

        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        if (body.contains("\"resultCode\"") &&
                body.contains("\"resultMsg\"") &&
                !body.contains("\"response\"")) {

            log.error("TourAPI 오류 응답: {}", body);
            return null;
        }

        try {
            TourApiResponse<TourApiItemsWrapper> apiResponse = objectMapper.readValue(
                    body,
                    objectMapper.getTypeFactory().constructParametricType(
                            TourApiResponse.class,
                            TourApiItemsWrapper.class
                    )
            );

            return validateResponse(apiResponse);
        } catch (Exception e) {
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, e.getMessage());
        }
    }

    private TourApiResponse<TourApiItemsWrapper> validateResponse(TourApiResponse<TourApiItemsWrapper> apiResponse) {
        if (apiResponse != null && apiResponse.response() != null && apiResponse.response().header() != null) {
            String resultCode = apiResponse.response().header().resultCode();
            String resultMsg = apiResponse.response().header().resultMsg();

            log.debug("TourAPI 응답 헤더 - resultCode: {}, resultMsg: {}", resultCode, resultMsg);

            if (TourApiConstants.RESPONSE_CODE_SUCCESS.equals(resultCode)) {
                return apiResponse;
            } else {
                String errorMessage = "TourAPI 오류: resultCode = " + resultCode + ", resultMsg = " + resultMsg;
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, errorMessage);
            }
        } else {
            log.warn("TourAPI 응답 구조 오류: 필수 필드 누락");
            return null;
        }
    }

    private String buildRestaurantListUrl(String encodedKey) {
        StringBuilder url = new StringBuilder(properties.getBaseUrl())
                .append(TourApiConstants.ENDPOINT_AREA_BASED_LIST)
                .append("?serviceKey=").append(encodedKey)
                .append("&numOfRows=1000")
                .append("&pageNo=").append(1)
                .append("&MobileOS=ETC")
                .append("&MobileApp=TOKTOT")
                .append("&_type=json")
                .append("&areaCode=39")
                .append("&cat1=").append(TourApiConstants.CAT1_FOOD);

        log.debug("TourAPI 요청 URL: {}", url.toString().replaceAll("serviceKey=[^&]*", "serviceKey=***"));
        return url.toString();
    }

    private HttpResponse<String> getStringHttpResponse(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getReadTimeout()))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }



    public TourApiResponse<TourApiDetailIntroWrapper> getRestaurantDetailIntro(String contentId) {
        log.info("DetailIntro API 호출: contentId={}", contentId);

        if (contentId == null || contentId.trim().isEmpty()) {
            log.warn("contentId가 비어있음");
            return null;
        }

        String encodedKey = URLEncoder.encode(properties.getServiceKey(), StandardCharsets.UTF_8);
        String url = buildDetailIntroUrl(encodedKey, contentId);
        long startTime = System.currentTimeMillis();

        try {
            HttpResponse<String> response = getStringHttpResponse(url);
            long responseTime = System.currentTimeMillis() - startTime;

            log.debug("DetailIntro API 응답: status={}, responseTime={}ms", response.statusCode(), responseTime);

            if (response.statusCode() == 200) {
                return processDetailIntroResponse(response.body(), responseTime, contentId);
            } else {
                log.error("DetailIntro API HTTP 오류: status={}, body={}", response.statusCode(), response.body());
            }
            return null;

        } catch (Exception e) {
            log.error("DetailIntro API 호출 예외: contentId={}, error={}", contentId, e.getMessage());
            return null;
        }
    }

    private TourApiResponse<TourApiDetailIntroWrapper> processDetailIntroResponse(String responseBody, long responseTime, String contentId) {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("DetailIntro API 빈 응답: contentId={}", contentId);
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode responseNode = rootNode.get("response");

            if (responseNode != null) {
                JsonNode headerNode = responseNode.get("header");
                if (headerNode != null) {
                    String resultCode = headerNode.get("resultCode").asText();
                    String resultMsg = headerNode.get("resultMsg").asText();
                    log.debug("DetailIntro API 헤더: resultCode={}, resultMsg={}", resultCode, resultMsg);
                }
            }

            TourApiResponse<TourApiDetailIntroWrapper> apiResponse = objectMapper.readValue(
                    responseBody,
                    objectMapper.getTypeFactory().constructParametricType(
                            TourApiResponse.class,
                            TourApiDetailIntroWrapper.class
                    )
            );

            if (apiResponse != null && apiResponse.response() != null && apiResponse.response().header() != null) {
                String resultCode = apiResponse.response().header().resultCode();

                if ("0000".equals(resultCode)) {
                    log.info("DetailIntro API 호출 성공: contentId={}, responseTime={}ms", contentId, responseTime);
                    return apiResponse;
                } else {
                    log.warn("DetailIntro API 오류 응답: contentId={}, resultCode={}", contentId, resultCode);
                }
            } else {
                log.warn("DetailIntro API 응답 구조 오류: contentId={}", contentId);
            }

            return null;

        } catch (Exception e) {
            log.error("DetailIntro API JSON 파싱 실패: contentId={}, error={}", contentId, e.getMessage());
            return null;
        }
    }

    private String buildDetailIntroUrl(String encodedKey, String contentId) {
        return new StringBuilder(properties.getBaseUrl())
                .append(TourApiConstants.ENDPOINT_DETAIL_INTRO)
                .append("?serviceKey=").append(encodedKey)
                .append("&MobileOS=ETC")
                .append("&MobileApp=AppTest")
                .append("&_type=json")
                .append("&contentId=").append(contentId)
                .append("&contentTypeId=39")
                .append("&numOfRows=1000")
                .append("&pageNo=1")
                .toString();
    }

}
