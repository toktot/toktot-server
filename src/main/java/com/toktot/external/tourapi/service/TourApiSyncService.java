package com.toktot.external.tourapi.service;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourApiSyncService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void saveOrUpdateRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            log.warn("유효하지 않은 레스토랑 데이터");
            return;
        }

        if (restaurantRepository.findByExternalTourApiId(restaurant.getExternalTourApiId()).isEmpty()) {
            restaurantRepository.save(restaurant);
            log.info("신규 매장 저장: id={}, name={}", restaurant.getId(), restaurant.getName());
        }
    }

}
