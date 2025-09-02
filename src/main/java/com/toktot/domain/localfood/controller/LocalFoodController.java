package com.toktot.domain.localfood.controller;

import com.toktot.domain.localfood.dto.LocalFoodResponse;
import com.toktot.domain.localfood.service.LocalFoodService;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/local-foods")
@RequiredArgsConstructor
public class LocalFoodController {

    private final LocalFoodService localFoodService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocalFoodResponse>>> readActiveLocalFoods() {
        log.atInfo()
                .setMessage("Local food list read request received")
                .log();

        List<LocalFoodResponse> response = localFoodService.getActiveLocalFoods();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
