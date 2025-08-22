package com.toktot.domain.review.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeywordCategory {
    PRICE("가격"),
    FOOD("음식"),
    SERVICE("서비스"),
    CLEANLINESS("청결"),
    ATMOSPHERE("분위기"),
    ACCESSIBILITY("주차공간"),

    ;
    private final String displayName;
}
