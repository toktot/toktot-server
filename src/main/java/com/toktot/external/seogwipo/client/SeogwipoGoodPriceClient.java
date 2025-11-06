package com.toktot.external.seogwipo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.seogwipo.config.SeogwipoGoodPriceProperties;
import com.toktot.external.seogwipo.dto.SeogwipoGoodPriceItem;
import com.toktot.external.seogwipo.dto.SeogwipoGoodPriceResponse;
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
public class SeogwipoGoodPriceClient {

    private final SeogwipoGoodPriceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public SeogwipoGoodPriceResponse getAllGoodPriceStores() {
        try {
            log.info("서귀포 착한가격업소 API 호출 시작 (JSON 방식)");

            List<SeogwipoGoodPriceItem> allItems = new ArrayList<>();
            int page = 1;
            int totalCount = 0;

            while (true) {
                SeogwipoGoodPriceResponse response = getGoodPriceStores(page);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("{}페이지에서 더 이상 데이터가 없음", page);
                    break;
                }

                if (page == 1) {
                    totalCount = response.getTotalCount();
                    log.info("서귀포 착한가격업소 총 {} 개 데이터 처리 예정", totalCount);
                }

                allItems.addAll(response.getData());
                log.debug("{}페이지 처리 완료: {} 개 항목 추가 (누적: {})",
                        page, response.getData().size(), allItems.size());

                if (allItems.size() >= totalCount) {
                    break;
                }

                page++;

                if (page > 100) {
                    log.warn("서귀포 착한가격업소 API 페이지 한계 도달: {}", page);
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SeogwipoGoodPriceResponse finalResponse = new SeogwipoGoodPriceResponse();
            finalResponse.setData(allItems);
            finalResponse.setTotalCount(allItems.size());
            finalResponse.setCurrentCount(allItems.size());

            log.info("서귀포 착한가격업소 API 호출 완료: 총 {} 개 데이터 수집", allItems.size());
            return finalResponse;

        } catch (Exception e) {
            log.error("서귀포 착한가격업소 API 호출 중 오류 발생", e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "서귀포 착한가격업소 API 호출 중 오류 발생: " + e.getMessage());
        }
    }

    private SeogwipoGoodPriceResponse getGoodPriceStores(int page) {
        try {
            String encodedServiceKey = URLEncoder.encode(properties.getServiceKey(), StandardCharsets.UTF_8);

            String url = UriComponentsBuilder
                    .fromUriString(properties.getApiUrl())
                    .queryParam("page", page)
                    .queryParam("perPage", properties.getPerPage())
                    .queryParam("serviceKey", encodedServiceKey)
                    .build()
                    .toString();

            log.debug("서귀포 착한가격업소 API 호출: page={}", page);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (compatible; ToktotBot/1.0)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("서귀포 착한가격업소 API 응답: statusCode={}, page={}",
                    response.statusCode(), page);

            if (response.statusCode() != 200) {
                log.error("서귀포 착한가격업소 API 호출 실패: statusCode={}, body={}",
                        response.statusCode(), response.body());
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "서귀포 착한가격업소 API 호출 실패: " + response.statusCode());
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "서귀포 착한가격업소 API 응답이 비어있음");
            }

            log.debug("서귀포 착한가격업소 API 응답 내용 (처음 500자): {}",
                    responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

            SeogwipoGoodPriceResponse result = objectMapper.readValue(responseBody, SeogwipoGoodPriceResponse.class);

            log.debug("서귀포 착한가격업소 API 파싱 성공: page={}, items={}",
                    page, result.getData() != null ? result.getData().size() : 0);

            return result;

        } catch (IOException | InterruptedException e) {
            log.error("서귀포 착한가격업소 API 호출 중 오류 발생: page={}", page, e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "서귀포 착한가격업소 API 호출 중 오류 발생: " + e.getMessage());
        }
    }
}
