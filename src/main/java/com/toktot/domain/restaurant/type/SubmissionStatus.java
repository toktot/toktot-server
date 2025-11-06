package com.toktot.domain.restaurant.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubmissionStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    APPROVED("처리 완료"),
    REJECTED("반려");
    ;

    private final String description;
}
