package com.toktot.domain.restaurant.service;

import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.external.kakao.service.KakaoMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantMatchService {

    private final RestaurantRepository restaurantRepository;
    private final KakaoMapService kakaoMapService;

    @Transactional
    public void addExternalKakaoIdInTourApiRestaurant() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("=== TourAPI 매장 카카오 ID 매칭 배치 시작 ===");

        var restaurants = restaurantRepository.findByExternalKakaoIdIsNull();
        int totalCount = restaurants.size();

        if (totalCount == 0) {
            log.info("처리할 매장이 없습니다. 배치를 종료합니다.");
            return;
        }

        log.info("처리 대상 매장 수: {}개", totalCount);

        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        int deactivatedCount = 0;

        try {
            for (var restaurant : restaurants) {
                processedCount++;

                try {
                    log.debug("매장 검색 중: {} (좌표: {}, {})",
                            restaurant.getName(),
                            restaurant.getLongitude(),
                            restaurant.getLatitude());

                    var searchResult = kakaoMapService.searchRestaurantByNameAndCoordinates(
                            restaurant.getName(),
                            restaurant.getLongitude(),
                            restaurant.getLatitude()
                    );

                    var placeInfoOptional = searchResult.placeInfos()
                            .stream()
                            .findFirst();

                    if (placeInfoOptional.isPresent()) {
                        var placeInfo = placeInfoOptional.get();
                        restaurant.setExternalKakaoId(placeInfo.getId());
                        successCount++;

                        log.debug("매장 매칭 성공: {} -> 카카오ID: {}",
                                restaurant.getName(), placeInfo.getId());

                    } else {
                        restaurant.setIsActive(false);
                        deactivatedCount++;

                        log.debug("매장 매칭 실패하여 비활성화: {}", restaurant.getName());
                    }

                    if (processedCount % Math.max(1, totalCount / 10) == 0) {
                        double progress = (double) processedCount / totalCount * 100;
                        log.info("진행률: {}/{} ({:.1f}%) - 성공: {}개, 비활성화: {}개",
                                processedCount, totalCount, progress, successCount, deactivatedCount);
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("매장 처리 중 오류 발생: {} - 오류: {}",
                            restaurant.getName(), e.getMessage(), e);
                }
            }

            Duration duration = Duration.between(startTime, LocalDateTime.now());
            log.info("=== TourAPI 매장 카카오 ID 매칭 배치 완료 ===");
            log.info("총 처리: {}개, 성공: {}개, 비활성화: {}개, 실패: {}개",
                    totalCount, successCount, deactivatedCount, failedCount);
            log.info("소요시간: {}분 {}초",
                    duration.toMinutes(), duration.getSeconds() % 60);

            if (totalCount > 0) {
                double successRate = (double) (successCount + deactivatedCount) / totalCount * 100;
                log.info("처리 성공률: {:.1f}% (매칭: {:.1f}%, 비활성화: {:.1f}%)",
                        successRate,
                        (double) successCount / totalCount * 100,
                        (double) deactivatedCount / totalCount * 100);
            }

        } catch (Exception e) {
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            log.error("=== TourAPI 매장 카카오 ID 매칭 배치 실패 ===");
            log.error("처리된 매장: {}/{}, 소요시간: {}분 {}초",
                    processedCount, totalCount,
                    duration.toMinutes(), duration.getSeconds() % 60);
            log.error("배치 실패 원인: {}", e.getMessage(), e);

            throw e;
        }
    }
}
