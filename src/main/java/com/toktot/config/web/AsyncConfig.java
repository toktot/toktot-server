package com.toktot.config.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Email-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("이메일 작업이 거부되었습니다. 큐가 가득 참 - 활성 스레드: {}, 큐 크기: {}",
                    executor1.getActiveCount(), executor1.getQueue().size());

            if (!executor1.isShutdown()) {
                runnable.run();
            }
        });

        executor.initialize();

        log.info("이메일 비동기 Executor 초기화 완료 - core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Async-");
        executor.setAwaitTerminationSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("비동기 작업이 거부되었습니다 - 활성 스레드: {}, 큐 크기: {}",
                    executor1.getActiveCount(), executor1.getQueue().size());

            if (!executor1.isShutdown()) {
                runnable.run();
            }
        });

        executor.initialize();

        log.info("일반 비동기 Executor 초기화 완료 - core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }
}
