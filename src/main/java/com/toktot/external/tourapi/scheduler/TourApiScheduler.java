package com.toktot.external.tourapi.scheduler;

import com.toktot.external.tourapi.TourApiService;
import com.toktot.external.tourapi.dto.BatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiScheduler {

    private final TourApiService tourApiService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    @Async
    public void scheduledTourApiSync() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("TourAPI 배치 동기화 시작 - {}", startTime);

        try {
            BatchResult result = tourApiService.collectAllJejuRestaurants();

            String endTime = LocalDateTime.now().format(FORMATTER);
            Duration duration = Duration.between(result.startTime(), result.endTime());

            if (result.isCompleted()) {
                log.info("TourAPI 배치 동기화 완료 - {}", endTime);
                log.info("처리 결과: 성공 {}개, 실패 {}개, 스킵 {}개",
                        result.successCount(), result.failureCount(), result.skipCount());
                log.info("소요시간: {}분 {}초",
                        duration.toMinutes(), duration.getSeconds() % 60);

                if (result.successCount() > 0) {
                    sendSuccessNotification(result);
                }
            } else {
                log.error("TourAPI 배치 동기화 실패 - {}", endTime);
                log.error("오류 내용: {}", result.errorMessage());
                sendFailureNotification(result.errorMessage());
            }

        } catch (Exception e) {
            String errorTime = LocalDateTime.now().format(FORMATTER);
            log.error("TourAPI 배치 동기화 예외 발생 - {} - {}", errorTime, e.getMessage(), e);
            sendFailureNotification("배치 작업 예외: " + e.getMessage());
        }
    }

    private void sendSuccessNotification(BatchResult result) {
        log.info("배치 작업 성공 알림");

        StringBuilder message = new StringBuilder();
        message.append("TourAPI 동기화 성공!\n");
        message.append("처리 결과:\n");
        message.append("- 성공: ").append(result.successCount()).append("건\n");
        message.append("- 실패: ").append(result.failureCount()).append("건\n");
        message.append("완료 시간: ").append(LocalDateTime.now().format(FORMATTER));

        log.info("알림 메시지:\n{}", message);
    }

    private void sendFailureNotification(String errorMessage) {
        log.error("배치 작업 실패: {}", errorMessage);

        StringBuilder message = new StringBuilder();
        message.append("TourAPI 동기화 실패!\n");
        message.append("오류 내용: ").append(errorMessage).append("\n");
        message.append("발생 시간: ").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        message.append("조치 필요: 서버 로그를 확인하고 수동 복구를 진행해주세요.");

        log.error("긴급 알림 메시지:\n{}", message);
    }

}
