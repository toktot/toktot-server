package com.toktot.external.tourapi;

import com.toktot.external.tourapi.dto.BatchResult;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.dto.TourApiRestaurant;
import com.toktot.external.tourapi.mapper.TourApiMapper;
import com.toktot.domain.restaurant.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {

    private final TourApiClient tourApiClient;
    private final TourApiMapper tourApiMapper;
    private final TourApiSyncService tourApiSyncService;

    public BatchResult collectAllJejuRestaurants() {
        log.info("제주 음식점 데이터 수집 시작");

        LocalDateTime startTime = LocalDateTime.now();
        int totalProcessed = 0;
        int successCount = 0;
        int failureCount = 0;
        int skipCount = 0;
        List<String> failedContentIds = new ArrayList<>();
        String errorMessage = null;

        try {
            List<TourApiRestaurant> allRestaurants = fetchAllPages();
            totalProcessed = allRestaurants.size();

            log.info("TourAPI에서 총 {}개 매장 데이터 수집 완료", totalProcessed);

            List<Restaurant> validRestaurants = new ArrayList<>();

            for (TourApiRestaurant dto : allRestaurants) {
                try {
                    Restaurant restaurant = tourApiMapper.toRestaurant(dto);

                    if (restaurant != null) {
                        validRestaurants.add(restaurant);
                        successCount++;
                    } else {
                        skipCount++;
                        log.debug("매장 데이터 변환 실패 (스킵): contentId={}", dto.contentId());
                    }
                } catch (Exception e) {
                    failureCount++;
                    failedContentIds.add(dto.contentId());
                    log.warn("매장 데이터 변환 중 오류: contentId={}, error={}",
                            dto.contentId(), e.getMessage());
                }
            }

            log.info("데이터 변환 완료 - 성공: {}, 스킵: {}, 실패: {}",
                    successCount, skipCount, failureCount);

            BatchResult syncResult = tourApiSyncService.saveRestaurantsBatch(validRestaurants);

            return new BatchResult(
                    totalProcessed,
                    syncResult.successCount(),
                    syncResult.failureCount() + failureCount,
                    syncResult.skipCount() + skipCount,
                    combineFailedIds(failedContentIds, syncResult.failedContentIds()),
                    startTime,
                    LocalDateTime.now(),
                    syncResult.errorMessage(),
                    true
            );

        } catch (Exception e) {
            errorMessage = "데이터 수집 중 예상치 못한 오류: " + e.getMessage();
            log.error("제주 음식점 데이터 수집 실패", e);

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

    private List<TourApiRestaurant> fetchAllPages() {
        List<TourApiRestaurant> allRestaurants = new ArrayList<>();
        int pageNo = 1;
        int totalCount = 0;

        try {
            while (true) {
                log.debug("TourAPI 페이지 {} 요청 중...", pageNo);

                TourApiResponse<TourApiItemsWrapper> response = tourApiClient.getAllJejuRestaurants(pageNo);

                if (response == null || response.response() == null || response.response().body() == null) {
                    log.warn("TourAPI 응답이 null입니다. 페이지: {}", pageNo);
                    break;
                }

                if (pageNo == 1) {
                    totalCount = response.response().body().totalCount();
                    log.info("TourAPI 전체 매장 수: {}개", totalCount);
                }

                TourApiItemsWrapper items = response.response().body().items();
                if (items == null || items.item() == null || items.item().isEmpty()) {
                    log.info("더 이상 데이터가 없습니다. 페이지: {}", pageNo);
                    break;
                }

                List<TourApiRestaurant> restaurants = items.item();
                allRestaurants.addAll(restaurants);

                log.debug("페이지 {} 완료: {}개 매장 수집 (누적: {}개)",
                        pageNo, restaurants.size(), allRestaurants.size());

                if (allRestaurants.size() >= totalCount) {
                    log.info("모든 데이터 수집 완료: {}개", allRestaurants.size());
                    break;
                }

                pageNo++;

                if (pageNo > 50) {
                    log.warn("최대 페이지 수 도달. 수집 중단: {}페이지", pageNo);
                    break;
                }
            }

        } catch (Exception e) {
            log.error("TourAPI 데이터 수집 중 오류 발생: 페이지={}, 수집된개수={}",
                    pageNo, allRestaurants.size(), e);
            throw e;
        }

        return allRestaurants;
    }

    private List<String> combineFailedIds(List<String> list1, List<String> list2) {
        List<String> combined = new ArrayList<>(list1);
        if (list2 != null) {
            combined.addAll(list2);
        }
        return combined;
    }

    public int getTotalRestaurantCount() {
        try {
            TourApiResponse<TourApiItemsWrapper> response = tourApiClient.getAllJejuRestaurants(1);

            if (response != null && response.response() != null && response.response().body() != null) {
                return response.response().body().totalCount();
            }

            return 0;
        } catch (Exception e) {
            log.warn("전체 매장 수 조회 실패", e);
            return 0;
        }
    }
}
