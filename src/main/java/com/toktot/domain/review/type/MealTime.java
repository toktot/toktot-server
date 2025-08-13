package com.toktot.domain.review.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MealTime {

    BREAKFAST("아침"),
    LUNCH("점심"),
    DINNER("저녁")
    ;

    private final String displayName;
}
