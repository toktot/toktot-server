package com.toktot.external.tourapi;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.tourapi.dto.BatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiSyncService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public BatchResult saveRestaurantsBatch(List<Restaurant> restaurants) {
        log.info("매장 배치 저장 시작: {}개", restaurants.size());

        LocalDateTime startTime = LocalDateTime.now();
        int totalProcessed = restaurants.size();
        int successCount = 0;
        int failureCount = 0;
        int skipCount = 0;
        List<String> failedContentIds = new ArrayList<>();
        String errorMessage = null;

        try {
            for (Restaurant restaurant : restaurants) {
                try {
                    SaveResult result = saveOrUpdateRestaurant(restaurant);

                    switch (result) {
                        case CREATED -> successCount++;
                        case UPDATED -> successCount++;
                        case SKIPPED -> skipCount++;
                        case FAILED -> {
                            failureCount++;
                            failedContentIds.add(restaurant.getExternalTourApiId());
                        }
                    }

                } catch (Exception e) {
                    failureCount++;
                    failedContentIds.add(restaurant.getExternalTourApiId());
                    log.warn("매장 저장 실패: contentId={}, error={}",
                            restaurant.getExternalTourApiId(), e.getMessage());
                }
            }

            log.info("매장 배치 저장 완료 - 성공: {}, 스킵: {}, 실패: {}",
                    successCount, skipCount, failureCount);

            return new BatchResult(
                    totalProcessed,
                    successCount,
                    failureCount,
                    skipCount,
                    failedContentIds,
                    startTime,
                    LocalDateTime.now(),
                    errorMessage,
                    true
            );

        } catch (Exception e) {
            errorMessage = "배치 저장 중 예상치 못한 오류: " + e.getMessage();
            log.error("매장 배치 저장 실패", e);

            return new BatchResult(
                    totalProcessed,
                    successCount,
                    failureCount,
                    skipCount,
                    failedContentIds,
                    startTime,
                    LocalDateTime.now(),
                    errorMessage,
                    false
            );
        }
    }

    @Transactional
    public SaveResult saveOrUpdateRestaurant(Restaurant restaurant) {
        if (restaurant == null || restaurant.getExternalTourApiId() == null) {
            log.debug("유효하지 않은 매장 데이터");
            return SaveResult.FAILED;
        }

        try {
            Optional<Restaurant> existingOpt = restaurantRepository
                    .findByExternalTourApiId(restaurant.getExternalTourApiId());

            if (existingOpt.isPresent()) {
                Restaurant existing = existingOpt.get();

                if (shouldUpdateRestaurant(existing, restaurant)) {
                    updateExistingRestaurant(existing, restaurant);
                    restaurantRepository.save(existing);

                    log.debug("기존 매장 업데이트: contentId={}, name={}",
                            restaurant.getExternalTourApiId(), restaurant.getName());
                    return SaveResult.UPDATED;
                } else {
                    log.debug("기존 매장 변경사항 없음: contentId={}",
                            restaurant.getExternalTourApiId());
                    return SaveResult.SKIPPED;
                }
            } else {
                Restaurant saved = restaurantRepository.save(restaurant);

                log.debug("신규 매장 저장: contentId={}, name={}, id={}",
                        restaurant.getExternalTourApiId(), restaurant.getName(), saved.getId());
                return SaveResult.CREATED;
            }

        } catch (Exception e) {
            log.warn("매장 저장/업데이트 실패: contentId={}, error={}",
                    restaurant.getExternalTourApiId(), e.getMessage());
            return SaveResult.FAILED;
        }
    }

    private boolean shouldUpdateRestaurant(Restaurant existing, Restaurant newData) {
        if (!equals(existing.getName(), newData.getName())) {
            return true;
        }

        if (!equals(existing.getAddress(), newData.getAddress())) {
            return true;
        }

        if (!equals(existing.getPhone(), newData.getPhone())) {
            return true;
        }

        if (!equals(existing.getCategory(), newData.getCategory())) {
            return true;
        }

        if (existing.getLatitude() != null && newData.getLatitude() != null) {
            if (existing.getLatitude().compareTo(newData.getLatitude()) != 0) {
                return true;
            }
        }

        if (existing.getLongitude() != null && newData.getLongitude() != null) {
            if (existing.getLongitude().compareTo(newData.getLongitude()) != 0) {
                return true;
            }
        }

        if (existing.getLastSyncedAt() == null ||
                existing.getLastSyncedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            return true;
        }

        return false;
    }

    private void updateExistingRestaurant(Restaurant existing, Restaurant newData) {
        existing.updateFromTourApi(newData);
    }

    private boolean equals(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }

    @Transactional(readOnly = true)
    public long countTourApiRestaurants() {
        return restaurantRepository.countByDataSource(DataSource.TOUR_API);
    }

    @Transactional(readOnly = true)
    public long countRecentlySynced(LocalDateTime since) {
        return restaurantRepository.countByDataSourceAndLastSyncedAtAfter(DataSource.TOUR_API, since);
    }

    public enum SaveResult {
        CREATED,
        UPDATED,
        SKIPPED,
        FAILED
    }
}
