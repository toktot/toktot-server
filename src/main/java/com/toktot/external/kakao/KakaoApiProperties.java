package com.toktot.external.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kakao.map")
@Getter
@Setter
public class KakaoApiProperties {

    private String apiKey;
    private String baseUrl = "https://dapi.kakao.com";

    private Integer connectionTimeout = 5000;
    private Integer readTimeout = 10000;

    private Integer maxRetryCount = 3;
    private Long retryDelayMs = 1000L;

    private Integer defaultPageSize = 15;
    private Integer maxPageSize = 45;
    private Integer defaultRadius = 1000;
    private Integer maxRadius = 20000;

    private Boolean enableLogging = true;
}
