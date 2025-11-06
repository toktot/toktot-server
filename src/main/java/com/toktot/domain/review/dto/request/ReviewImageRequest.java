package com.toktot.domain.review.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ReviewImageRequest(
        @JsonProperty(value = "image_id", required = true)
        @NotBlank(message = "이미지 처리에 오류가 발생했습니다.")
        String imageId,

        @JsonProperty(value = "order", required = true)
        @NotNull(message = "이미지 처리에 오류가 발생했습니다.")
        @Min(value = 1, message = "이미지 순서에 오류가 발생했습니다.")
        @Max(value = 5, message = "이미지 순서에 오류가 발생했습니다.")
        Integer order,

        @JsonProperty("tooltips")
        @Size(max = 5, message = "하나의 이미지에는 최대 5개의 툴팁만 입력할 수 있습니다.")
        @Valid
        List<TooltipRequest> tooltips,

        @JsonProperty("is_main")
        Boolean isMain
) {
}
