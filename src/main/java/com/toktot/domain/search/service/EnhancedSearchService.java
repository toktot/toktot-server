package com.toktot.domain.search.service;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.localfood.dto.LocalFoodStatsResponse;
import com.toktot.domain.localfood.service.LocalFoodDetectionService;
import com.toktot.domain.localfood.service.LocalFoodStatisticsService;
import com.toktot.domain.search.dto.response.EnhancedSearchResponse;
import com.toktot.web.dto.request.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 향토음식 감지 및 통계 포함 검색 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedSearchService {

    private final LocalFoodDetectionService detectionService;
    private final LocalFoodStatisticsService statisticsService;

    public <T> EnhancedSearchResponse<T> enhanceWithLocalFoodStats(SearchRequest request, T searchResult) {
        if (request.hasLocalFoodFilter()) {
            LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(request.localFood().type());
            return EnhancedSearchResponse.withLocalFood(searchResult, stats);
        }

        if (request.hasQuery()) {
            Optional<LocalFoodType> detected = detectionService.detectFromMenuName(request.query());
            if (detected.isPresent()) {
                LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(detected.get());
                return EnhancedSearchResponse.withLocalFood(searchResult, stats);
            }
        }

        return EnhancedSearchResponse.withoutLocalFood(searchResult);
    }

    public boolean isLocalFoodSearch(String query) {
        return query != null && detectionService.isLocalFood(query);
    }
}
