package com.toktot.interceptor.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.exception.ToktotException;
import com.toktot.common.exception.ErrorCode;
import com.toktot.domain.review.dto.response.search.PopularReviewResponse;
import com.toktot.domain.review.service.PopularReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewScheduler {

    private final PopularReviewService popularReviewService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String POPULAR_REVIEWS_KEY = "popular:reviews";

    @Scheduled(cron = "0 0 * * * *")
    public void cachePopularReviews() {
        log.info("많이 저장된 리뷰 스케줄러 시작: Redis 캐싱 작업을 수행합니다.");
        try {
            List<PopularReviewResponse> popularReviews = popularReviewService.getPopularReviews();

            String popularReviewsJson = serializeToJson(popularReviews);

            Duration ttl = Duration.ofHours(24);
            redisTemplate.opsForValue().set(POPULAR_REVIEWS_KEY, popularReviewsJson, ttl);

            log.info("많이 저장된 리뷰 {}개를 성공적으로 Redis에 캐싱했습니다. (Key: {})",
                    popularReviews.size(), POPULAR_REVIEWS_KEY);
        } catch (Exception e) {
            log.error("많이 저장된 리뷰 캐싱 스케줄 중 오류가 발생했습니다.", e);
        }
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패", e);
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "데이터를 JSON으로 변환하는데 실패했습니다.");
        }
    }
}