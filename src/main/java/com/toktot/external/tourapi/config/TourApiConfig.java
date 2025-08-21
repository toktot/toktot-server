package com.toktot.external.tourapi.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TourApiConfig {

    private final TourApiProperties tourApiProperties;

    @Bean(name = "tourApiWebClient")
    public WebClient tourApiWebClient() {
        return WebClient.builder()
                .baseUrl(tourApiProperties.getBaseUrl())
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(createObjectMapper()));
                })
                .build();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        mapper.coercionConfigFor(LogicalType.POJO)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);

        return mapper;
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("TourAPI 요청: {} {}", clientRequest.method(), clientRequest.url());
            return reactor.core.publisher.Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("TourAPI 응답: {}", clientResponse.statusCode());
            return reactor.core.publisher.Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is4xxClientError()) {
                log.warn("TourAPI 클라이언트 오류: {}", clientResponse.statusCode());
                if (clientResponse.statusCode().value() == 401) {
                    throw new RuntimeException("TourAPI 인증 실패: 서비스키를 확인해주세요.");
                } else if (clientResponse.statusCode().value() == 429) {
                    throw new RuntimeException("TourAPI 호출 제한 초과: 잠시 후 다시 시도해주세요.");
                }
            } else if (clientResponse.statusCode().is5xxServerError()) {
                log.error("TourAPI 서버 오류: {}", clientResponse.statusCode());
                throw new RuntimeException("TourAPI 서버 오류: 잠시 후 다시 시도해주세요.");
            }
            return reactor.core.publisher.Mono.just(clientResponse);
        });
    }
}
