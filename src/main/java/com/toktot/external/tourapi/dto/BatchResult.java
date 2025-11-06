package com.toktot.external.tourapi.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BatchResult(
        int totalProcessed,
        int successCount,
        int failureCount,
        int skipCount,
        List<String> failedContentIds,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String errorMessage,
        boolean isCompleted
) {

}