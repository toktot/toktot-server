package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.RestaurantMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantMenuRepository extends JpaRepository<RestaurantMenu, Long> {

    List<RestaurantMenu> findByRestaurantIdAndIsActiveTrueOrderByMenuName(Long restaurantId);
}
