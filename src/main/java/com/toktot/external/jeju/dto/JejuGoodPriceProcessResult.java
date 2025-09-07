package com.toktot.external.jeju.dto;

public record JejuGoodPriceProcessResult(
        int totalCount,
        int successCount,
        int skipCount,
        int errorCount
) {
    public static JejuGoodPriceProcessResult create() {
        return new JejuGoodPriceProcessResult(0, 0, 0, 0);
    }

    public JejuGoodPriceProcessResult incrementTotal() {
        return new JejuGoodPriceProcessResult(totalCount + 1, successCount, skipCount, errorCount);
    }

    public JejuGoodPriceProcessResult incrementSuccess() {
        return new JejuGoodPriceProcessResult(totalCount, successCount + 1, skipCount, errorCount);
    }

    public JejuGoodPriceProcessResult incrementSkip() {
        return new JejuGoodPriceProcessResult(totalCount, successCount, skipCount + 1, errorCount);
    }

    public JejuGoodPriceProcessResult incrementError() {
        return new JejuGoodPriceProcessResult(totalCount, successCount, skipCount, errorCount + 1);
    }
}
