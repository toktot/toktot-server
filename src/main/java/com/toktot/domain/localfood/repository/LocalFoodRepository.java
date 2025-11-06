package com.toktot.domain.localfood.repository;

import com.toktot.domain.localfood.LocalFood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalFoodRepository extends JpaRepository<LocalFood, Long> {

    List<LocalFood> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);

}
