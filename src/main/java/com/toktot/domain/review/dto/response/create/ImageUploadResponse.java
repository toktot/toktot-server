package com.toktot.domain.review.dto.response.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record ImageUploadResponse(
        @JsonProperty("uploaded_images")
        List<ImageInfoResponse> uploadedImages,

        @JsonProperty("total_image_count")
        int totalImageCount,

        @JsonProperty("remaining_slots")
        int remainingSlots,

        @JsonProperty("all_images")
        List<ImageInfoResponse> allImages
) {
    public static ImageUploadResponse from(List<ReviewImageDTO> uploadedImages, ReviewSessionDTO session) {
        List<ImageInfoResponse> uploadedImageInfos = uploadedImages.stream()
                .map(ImageInfoResponse::from)
                .toList();

        List<ImageInfoResponse> allImageInfos = session.getImages().stream()
                .map(ImageInfoResponse::from)
                .toList();

        return ImageUploadResponse.builder()
                .uploadedImages(uploadedImageInfos)
                .totalImageCount(session.getImageCount())
                .remainingSlots(5 - session.getImageCount())
                .allImages(allImageInfos)
                .build();
    }
}
