package com.toktot.external.tourapi.scheduler;

import com.toktot.domain.restaurant.service.RestaurantMatchService;
import com.toktot.external.tourapi.TourApiService;
import com.toktot.external.tourapi.dto.BatchResult;
import com.toktot.external.tourapi.service.TourApiDetailIntroService;
import com.toktot.external.tourapi.service.TourApiImageService;
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
    private final TourApiDetailIntroService tourApiDetailIntroService;
    private final TourApiImageService tourApiImageService;
    private final RestaurantMatchService restaurantMatchService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    @Async
    public void scheduledTourApiSync() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("TourAPI 기본정보 배치 동기화 시작 - {}", startTime);

        try {
            BatchResult result = tourApiService.collectAllJejuRestaurants();

            String endTime = LocalDateTime.now().format(FORMATTER);
            Duration duration = Duration.between(result.startTime(), result.endTime());

            if (result.isCompleted()) {
                log.info("TourAPI 기본정보 배치 동기화 완료 - {}", endTime);
                log.info("처리 결과: 성공 {}개, 실패 {}개, 스킵 {}개",
                        result.successCount(), result.failureCount(), result.skipCount());
                log.info("소요시간: {}분 {}초",
                        duration.toMinutes(), duration.getSeconds() % 60);

                if (result.successCount() > 0) {
                    sendSuccessNotification("기본정보", result);
                }
            } else {
                log.error("TourAPI 기본정보 배치 동기화 실패 - {}", endTime);
                log.error("오류 내용: {}", result.errorMessage());
                sendFailureNotification("기본정보 동기화", result.errorMessage());
            }

        } catch (Exception e) {
            String errorTime = LocalDateTime.now().format(FORMATTER);
            log.error("TourAPI 기본정보 배치 동기화 예외 발생 - {} - {}", errorTime, e.getMessage(), e);
            sendFailureNotification("기본정보 배치", "배치 작업 예외: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Async
    public void scheduledDetailIntroSync() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("TourAPI DetailIntro 배치 동기화 시작 - {}", startTime);

        try {
            LocalDateTime batchStartTime = LocalDateTime.now();
            int syncedCount = tourApiDetailIntroService.syncAllRestaurantsDetailIntro();
            LocalDateTime batchEndTime = LocalDateTime.now();

            Duration duration = Duration.between(batchStartTime, batchEndTime);
            String endTime = LocalDateTime.now().format(FORMATTER);

            log.info("TourAPI DetailIntro 배치 동기화 완료 - {}", endTime);
            log.info("처리 결과: 성공 {}개", syncedCount);
            log.info("소요시간: {}분 {}초", duration.toMinutes(), duration.getSeconds() % 60);

            if (syncedCount > 0) {
                sendDetailIntroSuccessNotification(syncedCount, duration);
            }

        } catch (Exception e) {
            String errorTime = LocalDateTime.now().format(FORMATTER);
            log.error("TourAPI DetailIntro 배치 동기화 예외 발생 - {} - {}", errorTime, e.getMessage(), e);
            sendFailureNotification("DetailIntro 배치", "배치 작업 예외: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void syncRestaurantImages() {
        log.info("=== TourAPI 이미지 동기화 스케줄러 시작 ===");

        try {
            int successCount = tourApiImageService.syncAllRestaurantsImages();
            log.info("=== TourAPI 이미지 동기화 완료: {} 건 성공 ===", successCount);

        } catch (Exception e) {
            log.error("=== TourAPI 이미지 동기화 실패 ===", e);
        }
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void addExternalKakaoIdInTourApiRestaurant() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        log.info("TourAPI 카카오 ID 매칭 배치 스케줄러 시작 - {}", startTime);

        try {
            restaurantMatchService.addExternalKakaoIdInTourApiRestaurant();

            String endTime = LocalDateTime.now().format(FORMATTER);
            log.info("TourAPI 카카오 ID 매칭 배치 스케줄러 완료 - {}", endTime);

        } catch (Exception e) {
            String errorTime = LocalDateTime.now().format(FORMATTER);
            log.error("TourAPI 카카오 ID 매칭 배치 스케줄러 예외 발생 - {} - {}",
                    errorTime, e.getMessage(), e);
            sendFailureNotification("카카오 ID 매칭 배치", "배치 작업 예외: " + e.getMessage());
        }
    }

    private void sendSuccessNotification(String type, BatchResult result) {
        log.info("배치 작업 성공 알림");

        StringBuilder message = new StringBuilder();
        message.append("TourAPI ").append(type).append(" 동기화 성공!\n");
        message.append("처리 결과:\n");
        message.append("- 성공: ").append(result.successCount()).append("건\n");
        message.append("- 실패: ").append(result.failureCount()).append("건\n");
        message.append("완료 시간: ").append(LocalDateTime.now().format(FORMATTER));

        log.info("알림 메시지:\n{}", message);
    }

    private void sendDetailIntroSuccessNotification(int syncedCount, Duration duration) {
        log.info("DetailIntro 배치 작업 성공 알림");

        StringBuilder message = new StringBuilder();
        message.append("TourAPI DetailIntro 동기화 성공!\n");
        message.append("처리 결과:\n");
        message.append("- 성공: ").append(syncedCount).append("건\n");
        message.append("- 소요시간: ").append(duration.toMinutes()).append("분 ").append(duration.getSeconds() % 60).append("초\n");
        message.append("완료 시간: ").append(LocalDateTime.now().format(FORMATTER));

        log.info("알림 메시지:\n{}", message);
    }

    private void sendFailureNotification(String operation, String errorMessage) {
        log.error("배치 작업 실패: {}", errorMessage);

        StringBuilder message = new StringBuilder();
        message.append("TourAPI ").append(operation).append(" 실패!\n");
        message.append("오류 내용: ").append(errorMessage).append("\n");
        message.append("발생 시간: ").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        message.append("조치 필요: 서버 로그를 확인하고 수동 복구를 진행해주세요.");

        log.error("긴급 알림 메시지:\n{}", message);
    }
}
