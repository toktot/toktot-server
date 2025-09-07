
package com.toktot.external.jeju.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.jeju.config.JejuGoodPriceProperties;
import com.toktot.external.jeju.dto.JejuGoodPriceItem;
import com.toktot.external.jeju.dto.JejuGoodPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JejuGoodPriceClient {

    private final JejuGoodPriceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<JejuGoodPriceItem> getAllGoodPriceStores() {
        List<JejuGoodPriceItem> allItems = new ArrayList<>();
        int pageNo = 1;
        int totalCount = 0;

        try {
            log.info("제주시 착한가격업소 API 호출 시작");

            while (true) {
                JejuGoodPriceResponse response = getGoodPriceStores(pageNo);

                if (!response.isSuccess()) {
                    log.error("제주시 착한가격업소 API 호출 실패: resultCode={}, resultMsg={}",
                            response.getResponse().getHeader().getResultCode(),
                            response.getResponse().getHeader().getResultMsg());
                    break;
                }

                if (pageNo == 1) {
                    totalCount = response.getTotalCount();
                    log.info("제주시 착한가격업소 총 {} 개 데이터 처리 예정", totalCount);
                }

                List<JejuGoodPriceItem> items = response.getItems();
                if (items.isEmpty()) {
                    log.info("{}페이지에서 더 이상 데이터가 없음", pageNo);
                    break;
                }

                allItems.addAll(items);
                log.debug("{}페이지 처리 완료: {} 개 항목 추가 (누적: {})",
                        pageNo, items.size(), allItems.size());

                if (allItems.size() >= totalCount) {
                    break;
                }

                pageNo++;

                if (pageNo > 100) {
                    log.warn("제주시 착한가격업소 API 페이지 한계 도달: {}", pageNo);
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            log.info("제주시 착한가격업소 API 호출 완료: 총 {} 개 데이터 수집", allItems.size());
            return allItems;

        } catch (Exception e) {
            log.error("제주시 착한가격업소 API 호출 중 오류 발생", e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "제주시 착한가격업소 API 호출 중 오류 발생: " + e.getMessage());
        }
    }

    private JejuGoodPriceResponse getGoodPriceStores(int pageNo) {
        try {
            String encodedServiceKey = URLEncoder.encode(properties.getServiceKey(), StandardCharsets.UTF_8);

            String url = UriComponentsBuilder
                    .fromUriString(properties.getApiUrl())
                    .queryParam("serviceKey", encodedServiceKey)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", properties.getPageSize())
                    .build()
                    .toString();

            log.info("제주시 착한가격업소 API 호출: pageNo={}, url={}", pageNo, url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (compatible; ToktotBot/1.0)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("제주시 착한가격업소 API 응답: statusCode={}, pageNo={}",
                    response.statusCode(), pageNo);

            if (response.statusCode() != 200) {
                log.error("제주시 착한가격업소 API 호출 실패: statusCode={}, body={}",
                        response.statusCode(), response.body());
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "제주시 착한가격업소 API 호출 실패: " + response.statusCode());
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "제주시 착한가격업소 API 응답이 비어있음");
            }

            log.debug("제주시 착한가격업소 API 응답 내용 (처음 500자): {}",
                    responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

            JejuGoodPriceResponse result = objectMapper.readValue(responseBody, JejuGoodPriceResponse.class);

            log.debug("제주시 착한가격업소 API 파싱 성공: pageNo={}, items={}",
                    pageNo, result.getItems().size());

            return result;

        } catch (IOException | InterruptedException e) {
            log.error("제주시 착한가격업소 API 호출 중 오류 발생: pageNo={}", pageNo, e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "제주시 착한가격업소 API 호출 중 오류 발생: " + e.getMessage());
        }
    }
}
