package com.toktot.external.tourapi.service;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.tourapi.dto.TourApiDetailImage;
import com.toktot.external.tourapi.dto.TourApiDetailImageWrapper;
import com.toktot.external.tourapi.dto.TourApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiImageService {

    private final TourApiClient tourApiClient;
    private final RestaurantRepository restaurantRepository;

    public void updateRestaurantImage(String contentId) {
        try {
            TourApiResponse<TourApiDetailImageWrapper> response = tourApiClient.getRestaurantImages(contentId);

            if (response != null && response.response() != null && response.response().body() != null) {
                TourApiDetailImageWrapper wrapper = response.response().body().items();

                if (wrapper != null) {
                    TourApiDetailImage imageInfo = wrapper.getFirstItem();

                    if (imageInfo != null && imageInfo.originimgurl() != null) {
                        Restaurant restaurant = restaurantRepository.findByExternalTourApiId(contentId)
                                .orElse(null);

                        if (restaurant != null) {
                            if (restaurant.getImage() == null || restaurant.getImage().trim().isEmpty()) {
                                restaurant.setImage(imageInfo.originimgurl());
                                restaurantRepository.save(restaurant);
                                log.info("이미지 URL 저장 완료: contentId={}, imageUrl={}", contentId, imageInfo.originimgurl());
                            } else {
                                log.debug("이미지가 이미 존재함: contentId={}", contentId);
                            }
                        }
                    } else {
                        log.debug("이미지 정보 없음: contentId={}", contentId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("이미지 정보 업데이트 실패: contentId={}", contentId, e);
        }
    }

    @Transactional
    public int syncAllRestaurantsImages() {
        List<Restaurant> restaurants = restaurantRepository.findAllByDataSourceAndIsActive(
                DataSource.TOUR_API, true);

        int successCount = 0;
        int total = restaurants.size();

        log.info("이미지 동기화 시작: 총 {} 매장", total);

        for (Restaurant restaurant : restaurants) {
            String contentId = restaurant.getExternalTourApiId();
            if (contentId != null && (restaurant.getImage() == null || restaurant.getImage().trim().isEmpty())) {
                updateRestaurantImage(contentId);
                successCount++;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("이미지 동기화 중단됨");
                    break;
                }

                if (successCount % 50 == 0) {
                    log.info("이미지 동기화 진행률: {}/{}", successCount, total);
                }
            }
        }

        log.info("이미지 동기화 완료: {}/{} 성공", successCount, total);
        return successCount;
    }
}
