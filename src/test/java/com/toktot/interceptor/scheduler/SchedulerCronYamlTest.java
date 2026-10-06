package com.toktot.interceptor.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.toktot.interceptor.scheduler.SchedulerCronFixture.PREFIX;
import static com.toktot.interceptor.scheduler.SchedulerCronFixture.PRODUCTION_CRON_BY_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class SchedulerCronYamlTest {

    @Test
    void 기본_설정의_cron은_기존_운영_시각과_같다() throws IOException {
        Map<String, String> crons = loadSchedulerCrons("application.yml");

        assertThat(crons).containsExactlyInAnyOrderEntriesOf(PRODUCTION_CRON_BY_KEY);
    }

    @Test
    void 로컬_설정은_모든_cron을_비활성화한다() throws IOException {
        Map<String, String> crons = loadSchedulerCrons("application-local.yml");

        assertThat(crons).containsOnlyKeys(PRODUCTION_CRON_BY_KEY.keySet());
        assertThat(crons.values()).containsOnly("-");
    }

    @Test
    void 읽지_않는_tour_api_scheduler_설정은_없다() throws IOException {
        assertThat(propertyNames("application.yml")).noneMatch(name -> name.startsWith("tour-api.scheduler."));
        assertThat(propertyNames("application-local.yml")).noneMatch(name -> name.startsWith("tour-api.scheduler."));
    }

    private Map<String, String> loadSchedulerCrons(String file) throws IOException {
        EnumerablePropertySource<?> source = load(file);
        return Arrays.stream(source.getPropertyNames())
                .filter(name -> name.startsWith(PREFIX))
                .collect(Collectors.toMap(
                        name -> name.substring(PREFIX.length()),
                        name -> String.valueOf(source.getProperty(name))));
    }

    private List<String> propertyNames(String file) throws IOException {
        return Arrays.asList(load(file).getPropertyNames());
    }

    private EnumerablePropertySource<?> load(String file) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
        return (EnumerablePropertySource<?>) sources.get(0);
    }
}
