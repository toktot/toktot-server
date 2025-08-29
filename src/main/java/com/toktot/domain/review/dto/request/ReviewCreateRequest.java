package com.toktot.domain.review.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.review.type.KeywordType;
import com.toktot.domain.review.type.MealTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ReviewCreateRequest(
        @JsonProperty(value = "id", required = true)
        @NotNull(message = "가게를 선택해주세요.")
        @Positive(message = "가게를 선택해주세요.")
        Long id,

        @JsonProperty(value = "images", required = true)
        @NotEmpty(message = "이미지는 최소 1장 이상 업로드해주세요.")
        @Size(max = 5, message = "이미지는 최대 5장까지 업로드 가능합니다.")
        @Valid
        List<ReviewImageRequest> images,

        @JsonProperty(value = "keywords", required = true)
        @NotEmpty(message = "키워드를 선택해주세요.")
        List<KeywordType> keywords,

        @JsonProperty("meal_time")
        MealTime mealTime,

        @JsonProperty("value_for_money_score")
        @NotNull(message = "가심비를 입력해주세요.")
        @Min(value = 0, message = "가심비는 0 이상으로 입력해주세요.")
        @Max(value = 100, message = "가심비는 100 이하로 입력해주세요.")
        Integer valueForMoneyScore
) {
}
