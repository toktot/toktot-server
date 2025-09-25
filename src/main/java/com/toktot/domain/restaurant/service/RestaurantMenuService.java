package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.RestaurantMenu;
import com.toktot.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.toktot.domain.restaurant.repository.RestaurantMenuRepository;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantMenuService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMenuRepository restaurantMenuRepository;

    public List<RestaurantMenuResponse> getRestaurantMenus(Long restaurantId) {
        Restaurant restaurant = findRestaurant(restaurantId);

        List<RestaurantMenu> menus = restaurantMenuRepository
                .findByRestaurantIdAndIsActiveTrueOrderByMenuName(restaurantId);

        List<RestaurantMenuResponse> responses = menus.stream()
                .map(RestaurantMenuResponse::from)
                .toList();

        log.info("가게 메뉴 조회 완료 - restaurantId: {}, menuCount: {}", restaurantId, responses.size());
        return responses;
    }

    private Restaurant findRestaurant(Long restaurantId) {
        return restaurantRepository
                .findById(restaurantId)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND));
    }
}
