package com.toktot.interceptor.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class LocalProfileSchedulerDisabledTest {

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Autowired
    private ReviewScheduler reviewScheduler;

    @Test
    void 로컬_프로파일에서는_cron_작업이_등록되지_않는다() {
        assertThat(scheduledTaskHolder.getScheduledTasks())
                .map(scheduledTask -> scheduledTask.getTask())
                .filteredOn(CronTask.class::isInstance)
                .isEmpty();
    }

    @Test
    void 스케줄러_빈은_수동_트리거를_위해_남아_있다() {
        assertThat(reviewScheduler).isNotNull();
    }
}
