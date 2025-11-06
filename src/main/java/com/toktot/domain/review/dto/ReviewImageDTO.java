package com.toktot.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.toktot.common.util.DateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageDTO {
    private String imageId;
    private String s3Key;
    private String imageUrl;
    private long fileSize;
    private int order;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    public static ReviewImageDTO create(String imageId, String s3Key, String imageUrl, long fileSize, int order) {
        return ReviewImageDTO.builder()
                .imageId(imageId)
                .s3Key(s3Key)
                .imageUrl(imageUrl)
                .fileSize(fileSize)
                .order(order)
                .uploadedAt(DateTimeUtil.nowWithoutNanos())
                .build();
    }
}
