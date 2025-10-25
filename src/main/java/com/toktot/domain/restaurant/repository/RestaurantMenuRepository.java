package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.RestaurantMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantMenuRepository extends JpaRepository<RestaurantMenu, Long> {

    List<RestaurantMenu> findByRestaurantIdAndIsActiveTrueOrderByMenuName(Long restaurantId);

    @Query("""
        SELECT rm.pricePerServing
        FROM RestaurantMenu rm
        WHERE rm.restaurant.id = :restaurantId
          AND rm.isMain = true
          AND rm.isActive = true
        ORDER BY rm.id ASC
        LIMIT 1
        """)
    Integer findMainMenuPricePerServing(@Param("restaurantId") Long restaurantId);
}
