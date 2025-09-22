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

    /**
     * 검색 요청에서 향토음식 감지 및 통계 포함 응답 생성
     */
    public <T> EnhancedSearchResponse<T> enhanceWithLocalFoodStats(SearchRequest request, T searchResult) {
        // 1. 명시적 향토음식 필터가 있는 경우
        if (request.hasLocalFoodFilter()) {
            LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(request.localFood().type());
            return EnhancedSearchResponse.withLocalFood(searchResult, stats);
        }

        // 2. 검색어에서 향토음식 자동 감지
        if (request.hasQuery()) {
            Optional<LocalFoodType> detected = detectionService.detectFromMenuName(request.query());
            if (detected.isPresent()) {
                LocalFoodStatsResponse stats = statisticsService.calculatePriceStats(detected.get());
                return EnhancedSearchResponse.withLocalFood(searchResult, stats);
            }
        }

        return EnhancedSearchResponse.withoutLocalFood(searchResult);
    }

    /**
     * 향토음식 검색 여부 확인
     */
    public boolean isLocalFoodSearch(SearchRequest request) {
        if (request.hasLocalFoodFilter()) {
            return true;
        }
        return request.hasQuery() && detectionService.isLocalFood(request.query());
    }
}
