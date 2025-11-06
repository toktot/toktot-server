package com.toktot.domain.review.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MealTime {

    BREAKFAST("아침"),
    LUNCH("점심"),
    DINNER("저녁"),
    ;

    private final String displayName;

    @JsonCreator
    public static MealTime from(String value) {
        try {
            return MealTime.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "meal_time type error");
        }
    }
}
