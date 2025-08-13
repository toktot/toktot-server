package com.toktot.domain.search.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchTab {

    RESTAURANTS("가게"),
    REVIEWS("리뷰"),

    ;

    private final String displayName;

}
