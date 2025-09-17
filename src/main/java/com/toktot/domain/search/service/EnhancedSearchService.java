package com.toktot.domain.search.service;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.localfood.dto.LocalFoodStatsResponse;
import com.toktot.domain.localfood.service.LocalFoodDetectionService;
import com.toktot.domain.localfood.service.LocalFoodStatisticsService;
import com.toktot.domain.search.dto.response.EnhancedSearchResponse;
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

    /**
     * 검색어에서 향토음식 감지 및 통계 포함 응답 생성
     */
    public <T> EnhancedSearchResponse<T> enhanceWithLocalFoodStats(String query, T searchResult) {
        if (query == null || query.trim().isEmpty()) {
            return EnhancedSearchResponse.withoutLocalFood(searchResult);
        }

        Optional<LocalFoodType> localFoodType = detectionService.detectFromMenuName(query);

        if (localFoodType.isPresent()) {
            LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(localFoodType.get());
            return EnhancedSearchResponse.withLocalFood(searchResult, stats);
        }

        return EnhancedSearchResponse.withoutLocalFood(searchResult);
    }

    /**
     * 향토음식 검색 여부 확인
     */
    public boolean isLocalFoodSearch(String query) {
        return query != null && detectionService.isLocalFood(query);
    }
}
