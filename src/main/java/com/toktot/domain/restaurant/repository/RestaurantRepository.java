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

    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.isActive = true")
    Integer countActiveRestaurants();

    @Query("SELECT COUNT(DISTINCT r) FROM Restaurant r " +
            "LEFT JOIN RestaurantMenu rm ON rm.restaurant.id = r.id AND rm.isMain = true " +
            "WHERE r.isActive = true AND " +
            "(rm.price IS NULL OR rm.price < :price OR " +
            "(rm.price IS NULL AND EXISTS (" +
            "  SELECT t FROM Tooltip t JOIN t.reviewImage ri JOIN ri.review rev " +
            "  WHERE rev.restaurant.id = r.id AND t.totalPrice < :price)))")
    Integer countRestaurantsWithCheaperRepresentativeMenu(@Param("price") Integer price);

    @Query("SELECT r.popularMenus FROM Restaurant r WHERE r.id = :restaurantId")
    Optional<String> findPopularMenusByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT t.totalPrice FROM Tooltip t " +
            "JOIN t.reviewImage ri JOIN ri.review r " +
            "WHERE r.restaurant.id = :restaurantId " +
            "AND t.totalPrice IS NOT NULL " +
            "GROUP BY t.totalPrice " +
            "ORDER BY COUNT(t) DESC")
    Integer findMostReviewedMenuPriceByRestaurantId(@Param("restaurantId") Long restaurantId);

    Optional<Restaurant> findByExternalTourApiId(String externalTourApiId);

    List<Restaurant> findByExternalKakaoIdIsNull();

    List<Restaurant> findByIsGoodPriceStoreAndIsActive(Boolean isGoodPriceStore, Boolean isActive);
}
