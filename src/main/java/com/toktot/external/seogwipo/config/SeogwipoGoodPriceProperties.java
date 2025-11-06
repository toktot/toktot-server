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

    private String serviceKey;
    private String apiUrl = "https://api.odcloud.kr/api/15000484/v1/uddi:7671d6fc-5827-487a-912e-c1176f1194f7";
    private int perPage = 100;
    private int connectTimeoutSeconds = 10;
    private int requestTimeoutSeconds = 30;
    private boolean enabledBatch = true;
}
