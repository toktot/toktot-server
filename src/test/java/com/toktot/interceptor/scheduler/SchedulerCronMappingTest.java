package com.toktot.interceptor.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.stream.Collectors;

import static com.toktot.interceptor.scheduler.SchedulerCronFixture.KEY_BY_METHOD;
import static com.toktot.interceptor.scheduler.SchedulerCronFixture.PRODUCTION_CRON_BY_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "toktot.scheduler.cron.tour-api-base-data=0 0 2 * * *",
        "toktot.scheduler.cron.tour-api-detail-intro=0 20 2 * * *",
        "toktot.scheduler.cron.tour-api-detail-common=0 40 2 * * *",
        "toktot.scheduler.cron.kakao-match=0 0 3 * * *",
        "toktot.scheduler.cron.seogwipo-good-price=0 20 3 * * *",
        "toktot.scheduler.cron.jeju-good-price=0 30 3 * * *",
        "toktot.scheduler.cron.good-price-cache=0 40 3 * * *",
        "toktot.scheduler.cron.popular-review=0 0 * * * *"
})
@ActiveProfiles("local")
class SchedulerCronMappingTest {

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Test
    void 각_스케줄러_메서드는_자기_cron_키의_시각으로_등록된다() {
        Map<String, String> cronByMethod = scheduledTaskHolder.getScheduledTasks().stream()
                .map(scheduledTask -> scheduledTask.getTask())
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .collect(Collectors.toMap(this::methodName, CronTask::getExpression));

        Map<String, String> expected = KEY_BY_METHOD.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> PRODUCTION_CRON_BY_KEY.get(entry.getValue())));

        assertThat(cronByMethod).containsExactlyInAnyOrderEntriesOf(expected);
    }

    private String methodName(CronTask task) {
        String fullName = task.toString();
        String className = fullName.substring(0, fullName.lastIndexOf('.'));
        return className.substring(className.lastIndexOf('.') + 1) + fullName.substring(fullName.lastIndexOf('.'));
    }
}
