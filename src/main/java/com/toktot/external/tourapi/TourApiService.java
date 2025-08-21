package com.toktot.external.tourapi;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.external.tourapi.dto.BatchResult;
import com.toktot.external.tourapi.dto.TourApiItemsWrapper;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.dto.TourApiRestaurant;
import com.toktot.external.tourapi.mapper.TourApiMapper;
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
            int pageNo = 1;
            boolean hasMoreData = true;

            while (hasMoreData) {
                log.info("=== 페이지 {} 처리 시작 ===", pageNo);

                try {
                    TourApiResponse<TourApiItemsWrapper> response = tourApiClient.getAllJejuRestaurants(pageNo);

                    if (response == null || response.response() == null || response.response().body() == null) {
                        log.warn("페이지 {}에서 빈 응답 수신", pageNo);
                        break;
                    }

                    if (response.response().header() != null) {
                        log.info("API 응답 헤더 - resultCode: {}, resultMsg: {}",
                                response.response().header().resultCode(),
                                response.response().header().resultMsg());
                    }

                    if (response.response().body() != null) {
                        Integer totalCount = response.response().body().totalCount();
                        Integer numOfRows = response.response().body().numOfRows();
                        Integer currentPageNo = response.response().body().pageNo();

                        log.info("API 응답 바디 - totalCount: {}, numOfRows: {}, pageNo: {}",
                                totalCount, numOfRows, currentPageNo);
                    }

                    TourApiItemsWrapper itemsWrapper = response.response().body().items();
                    if (itemsWrapper == null) {
                        log.info("페이지 {}에서 items가 null. 수집 완료", pageNo);
                        hasMoreData = false;
                        continue;
                    }

                    if (itemsWrapper.item() == null || itemsWrapper.item().isEmpty()) {
                        log.info("페이지 {}에서 더 이상 데이터가 없음. 수집 완료", pageNo);
                        hasMoreData = false;
                        continue;
                    }

                    List<TourApiRestaurant> restaurants = itemsWrapper.item();
                    log.info("페이지 {}에서 {}개 매장 데이터 수신", pageNo, restaurants.size());

                    for (TourApiRestaurant tourApiRestaurant : restaurants) {
                        totalProcessed++;

                        log.debug("처리 중인 매장: contentId={}, title={}, lat={}, lng={}",
                                tourApiRestaurant.contentId(),
                                tourApiRestaurant.title(),
                                tourApiRestaurant.latitude(),
                                tourApiRestaurant.longitude());

                        try {
                            Restaurant restaurant = tourApiMapper.toRestaurant(tourApiRestaurant);

                            if (restaurant == null) {
                                skipCount++;
                                log.warn("매장 데이터 스킵 (매퍼에서 null 반환): contentId={}, title={}",
                                        tourApiRestaurant.contentId(), tourApiRestaurant.title());
                                continue;
                            }

                            tourApiSyncService.saveOrUpdateRestaurant(restaurant);
                            successCount++;

                            if (successCount % 50 == 0) {
                                log.info("진행 상황: {}개 성공, {}개 실패, {}개 스킵",
                                        successCount, failureCount, skipCount);
                            }

                        } catch (Exception e) {
                            failureCount++;
                            failedContentIds.add(tourApiRestaurant.contentId());
                            log.error("매장 저장 실패: contentId={}, title={}, error={}",
                                    tourApiRestaurant.contentId(), tourApiRestaurant.title(), e.getMessage());
                        }
                    }

                    Integer totalCount = response.response().body().totalCount();
                    if (totalCount != null && totalProcessed >= totalCount) {
                        hasMoreData = false;
                        log.info("전체 데이터 수집 완료: 총 {}개 중 {}개 처리", totalCount, totalProcessed);
                    } else {
                        pageNo++;
                        log.debug("다음 페이지로 진행: pageNo={}, 현재까지 처리: {}/{}",
                                pageNo, totalProcessed, totalCount);
                    }

                } catch (Exception e) {
                    log.error("페이지 {} 처리 중 오류 발생: {}", pageNo, e.getMessage());
                    errorMessage = "페이지 " + pageNo + " 처리 실패: " + e.getMessage();
                    break;
                }
            }

        } catch (Exception e) {
            log.error("데이터 수집 중 전체 오류 발생", e);
            errorMessage = "전체 수집 실패: " + e.getMessage();
        }

        LocalDateTime endTime = LocalDateTime.now();

        BatchResult result = new BatchResult(
                totalProcessed,
                successCount,
                failureCount,
                skipCount,
                failedContentIds,
                startTime,
                endTime,
                errorMessage,
                errorMessage == null
        );

        log.info("제주 음식점 데이터 수집 완료 - 처리: {}, 성공: {}, 실패: {}, 스킵: {}",
                totalProcessed, successCount, failureCount, skipCount);

        return result;
    }

    public TourApiResponse<TourApiItemsWrapper> getRestaurantsByPage(int pageNo) {
        log.info("페이지 {} 데이터 조회", pageNo);
        return tourApiClient.getAllJejuRestaurants(pageNo);
    }
}
