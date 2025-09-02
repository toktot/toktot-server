package com.toktot.domain.review.service;

import com.toktot.domain.review.dto.response.ReviewSearchResponse;
import com.toktot.domain.review.repository.ReviewSearchRepositoryCustom;
import com.toktot.web.dto.request.SearchCriteria;
import com.toktot.web.dto.request.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewSearchService {

    private final ReviewSearchRepositoryCustom reviewSearchRepository;
    private final ReviewFilterService reviewFilterService;

    public Page<ReviewSearchResponse> searchReviews(SearchRequest request, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        try {
            SearchCriteria criteria = reviewFilterService.validateAndConvert(request);

            log.info("리뷰 검색 시작: {}", reviewFilterService.buildSearchLogMessage(criteria));

            Page<ReviewSearchResponse> result = reviewSearchRepository.searchReviewsWithFilters(criteria, pageable);

            long endTime = System.currentTimeMillis();
            log.info("리뷰 검색 완료 - 결과 수: {}, 응답 시간: {}ms", result.getTotalElements(), endTime - startTime);

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("리뷰 검색 실패 - 응답 시간: {}ms, 오류: {}", endTime - startTime, e.getMessage(), e);
            throw e;
        }
    }

    public Page<ReviewSearchResponse> searchReviewsByQuery(String query, Pageable pageable) {
        SearchRequest request = new SearchRequest(query, null, null, null, null, null);
        return searchReviews(request, pageable);
    }

    public Page<ReviewSearchResponse> searchReviewsNearby(String query, Double latitude, Double longitude, Integer radius, Pageable pageable) {
        var locationFilter = new com.toktot.web.dto.request.LocationFilterRequest(latitude, longitude, radius);
        SearchRequest request = new SearchRequest(query, locationFilter, null, null, null, null);
        return searchReviews(request, pageable);
    }
}
