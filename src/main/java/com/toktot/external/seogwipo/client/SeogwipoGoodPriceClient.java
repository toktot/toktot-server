package com.toktot.external.seogwipo.client;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.seogwipo.config.SeogwipoGoodPriceProperties;
import com.toktot.external.seogwipo.dto.SeogwipoGoodPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeogwipoGoodPriceClient {

    private final SeogwipoGoodPriceProperties properties;
    private final XmlMapper xmlMapper = new XmlMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public SeogwipoGoodPriceResponse getAllGoodPriceStores() {
        try {
            log.info("서귀포 착한가격업소 API 호출 시작");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (compatible; ToktotBot/1.0)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("서귀포 착한가격업소 API 응답: statusCode={}, finalUri={}",
                    response.statusCode(), response.uri());

            if (response.statusCode() != 200) {
                log.error("서귀포 착한가격업소 API 호출 실패: statusCode={}, body={}",
                        response.statusCode(), response.body());
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "서귀포 착한가격업소 API 호출 실패: " + response.statusCode());
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.error("서귀포 착한가격업소 API 응답이 비어있음");
                throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "서귀포 착한가격업소 API 응답이 비어있음");
            }

            log.debug("서귀포 착한가격업소 API 응답 내용 (처음 500자): {}",
                    responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

            SeogwipoGoodPriceResponse result = xmlMapper.readValue(responseBody, SeogwipoGoodPriceResponse.class);

            log.info("서귀포 착한가격업소 API 호출 성공: total={}, items={}",
                    result.getTotal(), result.getItems() != null ? result.getItems().size() : 0);

            return result;

        } catch (IOException | InterruptedException e) {
            log.error("서귀포 착한가격업소 API 호출 중 오류 발생", e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "서귀포 착한가격업소 API 호출 중 오류 발생: " + e.getMessage());
        }
    }
}
