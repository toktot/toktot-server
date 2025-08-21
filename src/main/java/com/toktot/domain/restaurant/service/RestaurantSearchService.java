package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.dto.request.RestaurantSearchRequest;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponse;
import com.toktot.web.dto.restaurant.response.RestaurantSearchResponseTemp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantSearchService {

    private final KakaoMapService kakaoMapService;

    public List<RestaurantSearchResponseTemp> getRestaurantResponse() {
        return List.of();
    }

    public RestaurantSearchResponse searchFromKakaoWithPagination(RestaurantSearchRequest request) {
        validateSearchRequest(request);

        try {
            RestaurantSearchRequest currentRequest = request;
            KakaoPlaceSearchResponse finalResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(currentRequest);

            while (finalResponse.getPlaceInfos().size() < KakaoApiConstants.DEFAULT_SIZE && finalResponse.hasMorePages()) {
                currentRequest = currentRequest.nextPage();
                KakaoPlaceSearchResponse currentPageResponse = kakaoMapService.searchJejuAllFoodAndCafePlace(currentRequest);
                finalResponse = currentPageResponse.mergeWithPreviousResults(finalResponse);
            }

            return RestaurantSearchResponse.from(finalResponse, currentRequest.page());
        } catch (Exception e) {
            log.error("Kakao search failed for request: {}", request, e);
            throw new ToktotException(ErrorCode.KAKAO_LOCAL_SERVICE_ERROR, e.getMessage());
        }
    }

    private void validateSearchRequest(RestaurantSearchRequest request) {
        if (request.query() == null || request.query().trim().isEmpty()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
        }
    }

}