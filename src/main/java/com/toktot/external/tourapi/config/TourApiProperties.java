package com.toktot.external.tourapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tour-api")
public class TourApiProperties {

    private String serviceKey;
    private String baseUrl = "https://apis.data.go.kr/B551011/KorService2";
    private int connectionTimeout = 5000;
    private int readTimeout = 10000;
    private int maxRetries = 3;
    private long retryDelay = 1000;
    private int dailyCallLimit = 1000;
    private int maxPageSize = 100;
    private String jejuAreaCode = "39";
    private String restaurantContentTypeId = "39";
    private String responseType = "json";
    private String mobileOs = "ETC";
    private String mobileApp = "TOKTOT";
    private String listYn = "Y";
    private String arrange = "C";
    private boolean enableMetrics = true;
    private boolean enableErrorNotification = true;

}
