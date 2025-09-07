package com.toktot.external.jeju.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jeju.good-price")
public class JejuGoodPriceProperties {
    private String apiUrl = "https://apis.data.go.kr/6510000/goodPriceStoreService/getGoodPirceStoreList";
    private String serviceKey;
    private boolean enabledBatch = true;
    private int pageSize = 100;
}
