package com.toktot.domain.restaurant.repository;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByExternalTourApiId(String externalTourApiId);

    long countByDataSource(DataSource dataSource);

    long countByDataSourceAndLastSyncedAtAfter(DataSource dataSource, LocalDateTime after);


}
