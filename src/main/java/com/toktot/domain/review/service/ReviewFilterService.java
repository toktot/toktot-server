package com.toktot.domain.review.service;

import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.request.SearchCriteria;
import com.toktot.web.dto.request.SearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReviewFilterService {

    public SearchCriteria validateAndConvert(SearchRequest request) {
        validateLocationFilter(request);
        validateRatingFilter(request);
        validateLocalFoodFilter(request);
        validateKeywordFilter(request);
        validateSortFilter(request);

        return SearchCriteria.from(request);
    }

    private void validateLocationFilter(SearchRequest request) {
        if (request.hasLocationFilter()) {
            if (!request.location().isWithinJejuBounds()) {
                throw new IllegalArgumentException("제주도 지역 내에서만 위치 기반 검색이 가능합니다.");
            }

            if (!request.location().isValid()) {
                throw new IllegalArgumentException("올바르지 않은 위치 정보입니다.");
            }
        }
    }

    private void validateRatingFilter(SearchRequest request) {
        if (request.hasRatingFilter()) {
            if (!request.rating().isValid()) {
                throw new IllegalArgumentException("별점은 0.5 이상 5.0 이하여야 합니다.");
            }
        }
    }

    private void validateLocalFoodFilter(SearchRequest request) {
        if (request.hasLocalFoodFilter()) {
            if (!request.localFood().isValid()) {
                String errorMessage = request.localFood().getPriceRangeErrorMessage();
                throw new IllegalArgumentException(errorMessage != null ? errorMessage : "향토음식 필터 설정이 올바르지 않습니다.");
            }
        }
    }

    private void validateKeywordFilter(SearchRequest request) {
        if (request.hasKeywordFilter()) {
            if (request.keywords().size() > 10) {
                throw new IllegalArgumentException("키워드는 최대 10개까지 선택 가능합니다.");
            }

            for (String keyword : request.keywords()) {
                if (keyword == null || keyword.trim().isEmpty()) {
                    throw new IllegalArgumentException("유효하지 않은 키워드가 포함되어 있습니다.");
                }
                if (keyword.length() > 20) {
                    throw new IllegalArgumentException("키워드는 20자 이하여야 합니다.");
                }
            }
        }
    }

    private void validateSortFilter(SearchRequest request) {
        if (request.hasSortFilter()) {
            if (request.sort() == SortType.DISTANCE && !request.hasLocationFilter()) {
                throw new IllegalArgumentException("거리순 정렬을 위해서는 위치 정보가 필요합니다.");
            }
        }
    }
}
