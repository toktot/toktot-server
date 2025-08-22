package com.toktot.web.dto.review.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.review.type.TooltipType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TooltipRequest(
        @JsonProperty(value = "type", required = true)
        @NotNull(message = "툴팁 처리에 오류가 발생하였습니다.")
        TooltipType type,

        @JsonProperty(value = "x", required = true)
        @NotNull(message = "툴팁 처리에 오류가 발생하였습니다.")
        @DecimalMin(value = "0.0", message = "툴팁 위치가 올바르지 않습니다.")
        @DecimalMax(value = "100.0", message = "툴팁 위치가 올바르지 않습니다.")
        BigDecimal xPosition,

        @JsonProperty(value = "y", required = true)
        @NotNull(message = "툴팁 처리에 오류가 발생하였습니다.")
        @DecimalMin(value = "0.0", message = "툴팁 위치가 올바르지 않습니다.")
        @DecimalMax(value = "100.0", message = "툴팁 위치가 올바르지 않습니다.")
        BigDecimal yPosition,

        @JsonProperty(value = "rating", required = true)
        @NotNull(message = "별점은 필수입니다.")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
        BigDecimal rating,

        @JsonProperty("menu_name")
        @Size(max = 100, message = "메뉴명은 100자 이하로 입력해주세요.")
        String menuName,

        @JsonProperty("total_price")
        @Positive(message = "잘못된 가격입니다.")
        Integer totalPrice,

        @JsonProperty("serving_size")
        @Positive(message = "인분 수는 1 이상이어야 합니다.")
        Integer servingSize,

        @JsonProperty("detailed_review")
        @Size(max = 100, message = "상세 리뷰는 100자 이하로 입력해주세요.")
        String detailedReview
) {
}
