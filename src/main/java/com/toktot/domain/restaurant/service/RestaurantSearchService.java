package com.toktot.domain.restaurant.service;

import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.web.dto.restaurant.RestaurantSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantSearchService {

    private final RestaurantRepository restaurantRepository;

    public List<RestaurantSearchResponse> getRestaurantResponse() {
        return restaurantRepository.findAll()
                .stream()
                .map(RestaurantSearchResponse::from)
                .toList();
    }
}
