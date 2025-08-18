package com.toktot.web.controller.restaurant;

import com.toktot.domain.restaurant.service.RestaurantSearchService;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantSearchController {

    private final RestaurantSearchService restaurantSearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RestaurantSearchResponse>>> searchRestaurants() {
        List<RestaurantSearchResponse> response = restaurantSearchService.getRestaurantResponse();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
