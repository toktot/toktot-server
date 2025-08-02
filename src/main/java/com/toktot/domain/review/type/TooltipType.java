package com.toktot.domain.review.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TooltipType {
    FOOD("음식"),
    SERVICE("서비스"),
    CLEAN("청결도");

    private final String displayName;
}
