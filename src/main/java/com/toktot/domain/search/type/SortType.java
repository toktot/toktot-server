package com.toktot.domain.search.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
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

    @JsonCreator
    public static SortType from(String value) {
        try {
            return SortType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "sort type error");
        }
    }

    public String getSortForKakao() {
        if (this.equals(SortType.DISTANCE)) {
            return "distance";
        }

        return "accuracy";
    }

}
