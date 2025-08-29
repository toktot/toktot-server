package com.toktot.external.tourapi.service;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.tourapi.dto.TourApiDetailCommon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiDetailCommonService {

    private final TourApiClient tourApiClient;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public int syncAllRestaurantsDetailCommon() {
        List<Restaurant> restaurants = restaurantRepository.findAllByDataSourceAndIsActive(DataSource.TOUR_API, true);
        int success = 0;

        log.info("DetailCommon 배치 시작: {} 개 매장", restaurants.size());

        for (Restaurant restaurant : restaurants) {
            if (restaurant.getExternalTourApiId() != null) {
                try {
                    if (updateRestaurant(restaurant)) {
                        success++;
                    }
                    Thread.sleep(200);
                } catch (Exception e) {
                    log.error("매장 업데이트 실패: id={}", restaurant.getId(), e);
                }
            }
        }

        log.info("DetailCommon 배치 완료: {}/{} 성공", success, restaurants.size());
        return success;
    }

    private boolean updateRestaurant(Restaurant restaurant) {
        TourApiDetailCommon detail = tourApiClient.getRestaurantDetailCommon(restaurant.getExternalTourApiId());
        if (detail == null) {
            return false;
        }
        restaurant.updateTourApiDetailCommon(detail.phone(), detail.image(), detail.image2());

        return true;
    }
}

