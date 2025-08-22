package com.toktot.web.dto.review.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record SessionInfoResponse(
        @JsonProperty("external_kakao_id")
        Long externalKakaoId,

        @JsonProperty("images")
        List<ImageInfoResponse> images,

        @JsonProperty("total_image_count")
        int totalImageCount,

        @JsonProperty("remaining_slots")
        int remainingSlots,

        @JsonProperty("has_session")
        boolean hasSession
) {
    public static SessionInfoResponse from(ReviewSessionDTO session) {
        if (session == null) {
            return SessionInfoResponse.builder()
                    .hasSession(false)
                    .totalImageCount(0)
                    .remainingSlots(5)
                    .images(List.of())
                    .build();
        }

        List<ImageInfoResponse> imageInfos = session.getImages().stream()
                .map(ImageInfoResponse::from)
                .toList();

        return SessionInfoResponse.builder()
                .externalKakaoId(session.getExternalKakaoId())
                .images(imageInfos)
                .totalImageCount(session.getImageCount())
                .remainingSlots(5 - session.getImageCount())
                .hasSession(true)
                .build();
    }
}
