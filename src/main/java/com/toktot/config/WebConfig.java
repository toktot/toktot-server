package com.toktot.config;

import com.toktot.common.logging.RequestLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("HTTP 요청 로깅 인터셉터 등록");

        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/actuator/**"
                );
    }
}
