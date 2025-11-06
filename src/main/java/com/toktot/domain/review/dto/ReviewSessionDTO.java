package com.toktot.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toktot.common.util.DateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSessionDTO {

    private Long userId;
    private Long restaurantId;

    @Builder.Default
    private List<ReviewImageDTO> images = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModified;

    public void removeImage(String imageId) {
        if (images != null) {
            images.removeIf(img -> img.getImageId().equals(imageId));
            this.lastModified = DateTimeUtil.nowWithoutNanos();
        }
    }

    @JsonIgnore
    public int getImageCount() {
        return images != null ? images.size() : 0;
    }

    public boolean hasImage(String imageId) {
        return images != null && images.stream()
                .anyMatch(img -> img.getImageId().equals(imageId));
    }

    public static ReviewSessionDTO create(Long userId, Long restaurantId) {
        LocalDateTime now = DateTimeUtil.nowWithoutNanos();
        return ReviewSessionDTO.builder()
                .userId(userId)
                .restaurantId(restaurantId)
                .images(new ArrayList<>())
                .createdAt(now)
                .lastModified(now)
                .build();
    }
}
