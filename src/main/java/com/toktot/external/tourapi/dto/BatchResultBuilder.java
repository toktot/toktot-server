package com.toktot.external.tourapi.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BatchResultBuilder {
    private final LocalDateTime startTime = LocalDateTime.now();
    private int totalProcessed = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private int skipCount = 0;
    private final List<String> failedContentIds = new ArrayList<>();
    private String errorMessage = null;

    public void incrementTotal() { totalProcessed++; }
    public void incrementSuccess() { successCount++; }
    public void incrementFailed() { failureCount++; }
    public void incrementSkipped() { skipCount++; }
    public void addFailedContentId(String contentId) { failedContentIds.add(contentId); }
    public void setError(String error) { this.errorMessage = error; }

    public int getTotalProcessed() { return totalProcessed; }
    public int getSuccessCount() { return successCount; }
    public int getFailedCount() { return failureCount; }
    public int getSkippedCount() { return skipCount; }

    public BatchResult build() {
        return new BatchResult(
                totalProcessed, successCount, failureCount, skipCount,
                new ArrayList<>(failedContentIds), startTime, LocalDateTime.now(),
                errorMessage, errorMessage == null
        );
    }

    public BatchResult buildWithError(String error) {
        this.errorMessage = error;
        return new BatchResult(
                totalProcessed, successCount, failureCount, skipCount,
                new ArrayList<>(failedContentIds), startTime, LocalDateTime.now(),
                error, false
        );
    }
}
