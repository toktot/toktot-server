package com.toktot.external.tourapi;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.external.tourapi.dto.*;
import com.toktot.external.tourapi.mapper.TourApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {

    private final TourApiClient tourApiClient;
    private final TourApiMapper tourApiMapper;
    private final TourApiSyncService tourApiSyncService;

    private static final int PROGRESS_LOG_INTERVAL = 100;
    private static final int API_CALL_DELAY_MS = 100;

    public BatchResult collectAllJejuRestaurants() {
        BatchResultBuilder resultBuilder = new BatchResultBuilder();

        try {
            processAllPages(resultBuilder);
            return resultBuilder.build();
        } catch (Exception e) {
            log.error("데이터 수집 중 치명적 오류", e);
            return resultBuilder.buildWithError("전체 수집 실패: " + e.getMessage());
        }
    }

    private void processAllPages(BatchResultBuilder resultBuilder) {
        int pageNo = 1;
        boolean hasMoreData = true;

        while (hasMoreData) {
            try {
                log.debug("페이지 {} 처리 시작", pageNo);

                TourApiResponse<TourApiItemsWrapper> response = fetchPageData(pageNo);
                if (response == null) {
                    log.warn("페이지 {}에서 null 응답", pageNo);
                    break;
                }

                List<TourApiRestaurant> restaurants = extractRestaurants(response);
                if (restaurants.isEmpty()) {
                    log.debug("페이지 {}에서 데이터 없음", pageNo);
                    hasMoreData = false;
                    continue;
                }

                processRestaurantsInPage(restaurants, resultBuilder);

                if (isCollectionComplete(response, resultBuilder.getTotalProcessed())) {
                    hasMoreData = false;
                    log.debug("데이터 수집 완료: {}개 처리", resultBuilder.getTotalProcessed());
                } else {
                    pageNo++;
                    addApiCallDelay();
                }

            } catch (Exception e) {
                log.error("페이지 {} 처리 실패: {}", pageNo, e.getMessage());
                resultBuilder.setError("페이지 " + pageNo + " 처리 실패: " + e.getMessage());
                break;
            }
        }
    }

    private TourApiResponse<TourApiItemsWrapper> fetchPageData(int pageNo) {
        return tourApiClient.getAllJejuRestaurants(pageNo);
    }

    private List<TourApiRestaurant> extractRestaurants(TourApiResponse<TourApiItemsWrapper> response) {
        if (response.response() == null ||
                response.response().body() == null ||
                response.response().body().items() == null ||
                response.response().body().items().item() == null) {
            return new ArrayList<>();
        }

        List<TourApiRestaurant> restaurants = response.response().body().items().item();
        log.debug("페이지에서 {}개 매장 수신", restaurants.size());
        return restaurants;
    }

    private void processRestaurantsInPage(List<TourApiRestaurant> restaurants, BatchResultBuilder resultBuilder) {
        for (TourApiRestaurant tourApiRestaurant : restaurants) {
            processSingleRestaurant(tourApiRestaurant, resultBuilder);

            if (resultBuilder.getSuccessCount() % PROGRESS_LOG_INTERVAL == 0) {
                log.info("진행률: {}개 성공", resultBuilder.getSuccessCount());
            }
        }
    }

    private void processSingleRestaurant(TourApiRestaurant tourApiRestaurant, BatchResultBuilder resultBuilder) {
        resultBuilder.incrementTotal();

        try {
            Restaurant restaurant = tourApiMapper.toRestaurant(tourApiRestaurant);

            if (restaurant == null) {
                resultBuilder.incrementSkipped();
                resultBuilder.addFailedContentId(tourApiRestaurant.contentId());
                log.debug("매장 스킵: contentId={}", tourApiRestaurant.contentId());
                return;
            }

            tourApiSyncService.saveOrUpdateRestaurant(restaurant);
            resultBuilder.incrementSuccess();

        } catch (Exception e) {
            resultBuilder.incrementFailed();
            resultBuilder.addFailedContentId(tourApiRestaurant.contentId());
            log.error("매장 저장 실패: contentId={}, error={}",
                    tourApiRestaurant.contentId(), e.getMessage());
        }
    }

    private boolean isCollectionComplete(TourApiResponse<TourApiItemsWrapper> response, int totalProcessed) {
        if (response.response() == null || response.response().body() == null) {
            return true;
        }

        Integer totalCount = response.response().body().totalCount();
        return totalCount != null && totalProcessed >= totalCount;
    }

    private void addApiCallDelay() {
        try {
            Thread.sleep(API_CALL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("API 호출 지연 중 인터럽트");
        }
    }

    public TourApiResponse<TourApiItemsWrapper> getRestaurantsByPage(int pageNo) {
        log.debug("페이지 {} 조회 요청", pageNo);

        try {
            TourApiResponse<TourApiItemsWrapper> response = tourApiClient.getAllJejuRestaurants(pageNo);

            if (response != null && response.response() != null && response.response().body() != null) {
                int itemCount = extractRestaurants(response).size();
                log.debug("페이지 {} 조회 완료: {}개", pageNo, itemCount);
            }

            return response;
        } catch (Exception e) {
            log.error("페이지 {} 조회 실패: {}", pageNo, e.getMessage());
            throw new RuntimeException("페이지 조회 실패", e);
        }
    }
}
