package com.toktot.domain.review.controller;

import com.toktot.ToktotApplication;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.search.type.SortType;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.review.dto.response.ReviewSearchResponse;
import com.toktot.domain.review.service.ReviewSearchService;
import com.toktot.web.dto.request.SearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewSearchController {

    private final ReviewSearchService reviewSearchService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<ReviewSearchResponse>>> searchReviews(
            @RequestBody @Valid SearchRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewSearchResponse> result = reviewSearchService.searchReviews(request, createPageableWithSort(request, pageable));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Pageable createPageableWithSort(SearchRequest request, Pageable defaultPageable) {
        if (request.hasSortFilter()) {
            SortType sortType = request.sort();
            Sort sort = createSortFromType(sortType);

            return PageRequest.of(
                    defaultPageable.getPageNumber(),
                    defaultPageable.getPageSize(),
                    sort
            );
        }
        return defaultPageable;
    }

    private Sort createSortFromType(SortType sortType) {
        return switch (sortType) {
            case DISTANCE -> Sort.by(Sort.Direction.ASC, "distance");
            case POPULARITY -> Sort.by(Sort.Direction.DESC, "popularity");
            case RATING -> Sort.by(Sort.Direction.DESC, "rating");
            case SATISFACTION -> Sort.by(Sort.Direction.DESC, "satisfactionScore");
        };
    }

}
