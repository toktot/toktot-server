package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT r.popularMenus FROM Restaurant r WHERE r.id = :restaurantId")
    Optional<String> findPopularMenusByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT t.totalPrice FROM Tooltip t " +
            "JOIN t.reviewImage ri " +
            "JOIN ri.review r " +
            "WHERE r.restaurant.id = :restaurantId " +
            "AND t.totalPrice IS NOT NULL " +
            "AND r.isHidden = false " +
            "GROUP BY t.totalPrice " +
            "ORDER BY COUNT(t) DESC, t.totalPrice ASC")
    List<Integer> findMostReviewedMenuPricesByRestaurantId(
            @Param("restaurantId") Long restaurantId,
            Pageable pageable);

    Optional<Restaurant> findByExternalTourApiId(String externalTourApiId);

    List<Restaurant> findByExternalKakaoIdIsNull();

    List<Restaurant> findByIsGoodPriceStoreAndIsActive(Boolean isGoodPriceStore, Boolean isActive);
}
