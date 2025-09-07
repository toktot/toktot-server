package com.toktot.external.seogwipo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "seogwipo.good-price")
public class SeogwipoGoodPriceProperties {

    private String apiUrl = "https://www.seogwipo.go.kr/openapi/goodPriceService";
    private int connectTimeoutSeconds = 10;
    private int requestTimeoutSeconds = 30;
    private boolean enabledBatch = true;
}
