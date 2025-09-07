package com.toktot.domain.review.dto.response.search;

import com.toktot.domain.review.ReviewKeyword;
import lombok.Builder;

import java.util.List;

@Builder
public record PopularReviewResponse(
        Long id,
        ReviewAuthorResponse author,
        Boolean isBookmarked,
        Integer valueForMoneyScore,
        List<String> keywords,
        String imageUrl,
        ReviewRestaurantInfo restaurant,
        Double rating
) {

}
