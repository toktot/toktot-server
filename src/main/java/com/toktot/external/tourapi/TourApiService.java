package com.toktot.external.tourapi;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.external.tourapi.dto.*;
import com.toktot.external.tourapi.mapper.TourApiMapper;
import com.toktot.external.tourapi.service.TourApiClient;
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

    public void findRestaurantsInJeju() {
        TourApiResponse<TourApiItemsWrapper> response = tourApiClient.getAllJejuRestaurants();
        if (response == null) { return; }
        List<TourApiRestaurant> tourApiRestaurants = extractRestaurants(response);
        if (tourApiRestaurants.isEmpty()) { return; }

        for (TourApiRestaurant tourApiRestaurant : tourApiRestaurants) {
            try {
                Restaurant restaurant = tourApiMapper.toRestaurant(tourApiRestaurant);

                if (restaurant == null) {
                    return;
                }

                tourApiSyncService.saveOrUpdateRestaurant(restaurant);
            } catch (Exception e) {
                log.error("tour api data insert error = {}", e.getMessage());
            }
        }

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

}
