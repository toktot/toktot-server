package com.toktot.domain.report.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserReportType {

    SPAM_PROMOTIONAL("신뢰하기 어려운 홍보성 게시글을 다수 올림"),
    INAPPROPRIATE_CONTENT("음란성 또는 부적절한 게시글을 올림"),
    DEFAMATION_COPYRIGHT("명예훼손 및 저작권 침해"),
    OTHER("그 외 기타"),
    ;

    private final String description;

}
