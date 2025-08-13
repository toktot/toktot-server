package com.toktot.domain.report.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    APPROVED("처리 완료"),
    REJECTED("반려");

    private final String description;

}
