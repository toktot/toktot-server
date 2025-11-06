package com.toktot.external.seogwipo.dto;

public record SeogwipoGoodPriceProcessResult(
        int totalCount,
        int successCount,
        int skipCount,
        int errorCount
) {
    public static SeogwipoGoodPriceProcessResult create() {
        return new SeogwipoGoodPriceProcessResult(0, 0, 0, 0);
    }

    public SeogwipoGoodPriceProcessResult incrementTotal() {
        return new SeogwipoGoodPriceProcessResult(totalCount + 1, successCount, skipCount, errorCount);
    }

    public SeogwipoGoodPriceProcessResult incrementSuccess() {
        return new SeogwipoGoodPriceProcessResult(totalCount, successCount + 1, skipCount, errorCount);
    }

    public SeogwipoGoodPriceProcessResult incrementSkip() {
        return new SeogwipoGoodPriceProcessResult(totalCount, successCount, skipCount + 1, errorCount);
    }

    public SeogwipoGoodPriceProcessResult incrementError() {
        return new SeogwipoGoodPriceProcessResult(totalCount, successCount, skipCount, errorCount + 1);
    }
}
