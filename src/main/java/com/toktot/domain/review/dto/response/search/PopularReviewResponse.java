package com.toktot.domain.review.dto.response.search;

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
    public PopularReviewResponse withIsBookmarked(boolean isBookmarked) {
        return new PopularReviewResponse(
                this.id,
                this.author,
                isBookmarked,
                this.valueForMoneyScore,
                this.keywords,
                this.imageUrl,
                this.restaurant,
                this.rating
        );
    }
}
