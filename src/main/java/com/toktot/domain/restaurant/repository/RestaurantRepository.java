package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByExternalTourApiId(String externalTourApiId);

    Optional<Restaurant> findByExternalKakaoId(String externalKakaoId);

    List<Restaurant> findAllByDataSource(DataSource dataSource);

    List<Restaurant> findAllByDataSourceAndIsActive(DataSource dataSource, Boolean isActive);

    List<Restaurant> findByExternalKakaoIdIsNull();
}
