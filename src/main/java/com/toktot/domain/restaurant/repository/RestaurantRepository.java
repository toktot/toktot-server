package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByExternalKakaoIdAndIsActive(String externalKakaoId, Boolean isActive);

    Optional<Restaurant> findByNameAndIsActive(String name, Boolean isActive);

    List<Restaurant> findByNameContainingIgnoreCaseAndIsActive(String name, Boolean isActive);

    List<Restaurant> findAllByDataSourceAndIsActive(DataSource dataSource, Boolean isActive);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId AND r.isHidden = false")
    Long countReviewsByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT r.valueForMoneyScore FROM Review r WHERE r.restaurant.id = :restaurantId AND r.isHidden = false")
    List<Integer> findValueForMoneyScoresByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("""
        SELECT r.restaurant.id, r.valueForMoneyScore
        FROM Review r
        WHERE r.restaurant.id IN :restaurantIds
          AND r.isHidden = false
          AND r.valueForMoneyScore IS NOT NULL
        """)
    List<Object[]> findValueForMoneyScoresBatch(@Param("restaurantIds") List<Long> restaurantIds);

    @Query("""
        SELECT COUNT(DISTINCT rm.restaurant.id)
        FROM RestaurantMenu rm
        WHERE rm.isMain = true 
          AND rm.isActive = true
        """)
    Long countRestaurantsWithMainMenu();

    @Query("""
        SELECT COUNT(DISTINCT rm.restaurant.id)
        FROM RestaurantMenu rm
        WHERE rm.isMain = true
          AND rm.isActive = true
          AND rm.pricePerServing < :price
        """)
    Long countRestaurantsWithCheaperMainMenu(@Param("price") Integer price);

    Optional<Restaurant> findByExternalTourApiId(String externalTourApiId);

    List<Restaurant> findByExternalKakaoIdIsNull();

    List<Restaurant> findByIsGoodPriceStoreAndIsActive(Boolean isGoodPriceStore, Boolean isActive);
}
