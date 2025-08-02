package com.toktot.domain.review.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeywordCategory {
    ATMOSPHERE("분위기");

    private final String displayName;
}
