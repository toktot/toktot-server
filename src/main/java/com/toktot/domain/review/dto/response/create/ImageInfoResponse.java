package com.toktot.domain.review.dto.response.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.review.dto.ReviewImageDTO;
import lombok.Builder;

@Builder
public record ImageInfoResponse(
        @JsonProperty("image_id")
        String imageId,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("file_size")
        long fileSize,

        @JsonProperty("order")
        int order
) {
    public static ImageInfoResponse from(ReviewImageDTO dto) {
        return ImageInfoResponse.builder()
                .imageId(dto.getImageId())
                .imageUrl(dto.getImageUrl())
                .fileSize(dto.getFileSize())
                .order(dto.getOrder())
                .build();
    }
}
