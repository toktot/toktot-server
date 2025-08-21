package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByExternalTourApiId(String externalTourApiId);

    long countByDataSource(DataSource dataSource);
}
