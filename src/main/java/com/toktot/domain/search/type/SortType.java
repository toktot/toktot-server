package com.toktot.domain.search.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SortType {

    DISTANCE("거리순", "distance"),
    POPULARITY("인기순", "popularity"),
    RATING("평점순", "rating"),
    SATISFACTION("만족도순", "satisfaction"),

    ;

    private final String displayName;
    private final String queryParam;

}
