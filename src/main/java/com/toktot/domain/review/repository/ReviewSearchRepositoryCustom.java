package com.toktot.domain.review.repository;

import com.toktot.domain.review.dto.response.ReviewSearchResponse;
import com.toktot.web.dto.request.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewSearchRepositoryCustom {

    Page<ReviewSearchResponse> searchReviewsWithFilters(SearchCriteria criteria, Pageable pageable);
}
