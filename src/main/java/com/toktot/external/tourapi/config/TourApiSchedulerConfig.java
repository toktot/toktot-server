package com.toktot.external.tourapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
public class TourApiSchedulerConfig {

    @Value("${tour-api.batch.core-pool-size:2}")
    private int corePoolSize;

    @Value("${tour-api.batch.max-pool-size:5}")
    private int maxPoolSize;

    @Value("${tour-api.batch.queue-capacity:100}")
    private int queueCapacity;

    @Value("${tour-api.batch.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${tour-api.batch.await-termination-seconds:60}")
    private int awaitTerminationSeconds;

    @Bean("batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);

        executor.setThreadNamePrefix("TourAPI-Batch-");

        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        executor.initialize();

        log.info("TourAPI 배치 작업용 스레드 풀 설정 완료 - Core: {}, Max: {}, Queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    @Bean
    public SchedulerInfoLogger schedulerInfoLogger() {
        return new SchedulerInfoLogger();
    }

    public static class SchedulerInfoLogger {

        public SchedulerInfoLogger() {
            logSchedulerInfo();
        }

        private void logSchedulerInfo() {
            log.info("TourAPI 스케줄러 설정 완료");
            log.info("예정된 배치 작업: TourAPI 기본정보 동기화: 매일 새벽 2시 (cron: '0 0 2 * * *')");
            log.info("예정된 배치 작업: TourAPI DetailIntro 동기화: 매일 새벽 3시 (cron: '0 0 3 * * *')");
            log.info("예정된 배치 작업: TourAPI 이미지 동기화: 매일 새벽 4시 (cron: '0 0 4 * * *')");
            log.info("예정된 배치 작업: TourAPI & Kakao ID 동기화: 매일 새벽 5시 (cron: '0 0 5 * * *')");
        }
    }
}
