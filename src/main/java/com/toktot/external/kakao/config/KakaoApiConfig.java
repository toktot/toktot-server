package com.toktot.external.kakao.config;

import com.toktot.external.kakao.KakaoApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KakaoApiConfig {

    private final KakaoApiProperties kakaoApiProperties;

    @Bean("kakaoRestTemplate")
    public RestTemplate kakaoRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(kakaoApiProperties.getConnectionTimeout()))
                .setReadTimeout(Duration.ofMillis(kakaoApiProperties.getReadTimeout()))
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .errorHandler(kakaoApiErrorHandler())
                .messageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Bean
    public ResponseErrorHandler kakaoApiErrorHandler() {
        return new KakaoApiErrorHandler();
    }
}
