package com.toktot.external.tourapi;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiSyncService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public Restaurant saveOrUpdateRestaurant(Restaurant restaurant) {
        if (restaurant == null || restaurant.getExternalTourApiId() == null) {
            log.warn("유효하지 않은 레스토랑 데이터");
            return null;
        }

        Optional<Restaurant> existingOptional = restaurantRepository.findByExternalTourApiId(
                restaurant.getExternalTourApiId()
        );

        if (existingOptional.isPresent()) {
            Restaurant existing = existingOptional.get();
            log.debug("기존 매장 발견: id={}, contentId={}", existing.getId(), existing.getExternalTourApiId());

            if (existing.hasDataChangedFrom(restaurant)) {
                existing.updateFromTourApiData(restaurant);
                Restaurant saved = restaurantRepository.save(existing);
                log.info("기존 매장 업데이트: id={}, name={}", saved.getId(), saved.getName());
                return saved;
            } else {
                existing.updateSyncTime();
                Restaurant saved = restaurantRepository.save(existing);
                log.debug("매장 정보 변경 없음: id={}, name={}", saved.getId(), saved.getName());
                return saved;
            }
        } else {
            restaurant.updateSyncTime();
            Restaurant saved = restaurantRepository.save(restaurant);
            log.info("신규 매장 저장: id={}, name={}", saved.getId(), saved.getName());
            return saved;
        }
    }

    @Transactional(readOnly = true)
    public long countTourApiRestaurants() {
        return restaurantRepository.countByDataSource(DataSource.TOUR_API);
    }

}
