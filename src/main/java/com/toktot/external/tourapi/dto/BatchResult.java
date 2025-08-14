package com.toktot.external.tourapi.dto;

import java.time.LocalDateTime;
import java.util.List;

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