package com.toktot.domain.report.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewReportType {

    MALICIOUS_POST("근거 없이 악의적인 게시글"),
    PROMOTIONAL_POST("신뢰하기 어려운 홍보성 게시글"),
    INAPPROPRIATE_CONTENT("음란성 또는 부적절한 게시글"),
    DEFAMATION_COPYRIGHT("명예훼손 및 저작권 침해"),
    WRONG_RESTAURANT("다른 매장의 리뷰"),
    PRIVACY_VIOLATION("초상권 침해 또는 개인정보 노출"),
    OTHER("그 외 기타");

    private final String description;

}
