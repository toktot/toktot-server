package com.toktot.web.dto.review.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.review.type.KeywordType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ReviewCreateRequest(
        @JsonProperty(value = "external_kakao_id", required = true)
        @NotNull(message = "존재하지 않은 가게입니다.")
        @Positive(message = "올바른 음식점 ID를 입력해주세요.")
        Long externalKakaoId,

        @JsonProperty(value = "keywords", required = true)
        @NotEmpty(message = "키워드를 선택해주세요.")
        List<KeywordType> keywords,

        @JsonProperty(value = "images", required = true)
        @NotEmpty(message = "이미지는 최소 1장 이상 업로드해주세요.")
        @Size(max = 5, message = "이미지는 최대 5장까지 업로드 가능합니다.")
        @Valid
        List<ReviewImageRequest> images
) {
}
