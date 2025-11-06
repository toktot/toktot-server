package com.toktot.domain.report.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReporterType {

    CUSTOMER("일반 고객"),
    RESTAURANT_OWNER("매장 관계자"),
    ;

    private final String description;

}
