package com.toktot.domain.review.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeywordType {
    // 분위기 관련
    OCEAN_VIEW("오션뷰", KeywordCategory.ATMOSPHERE),
    CITY_VIEW("도시가 보이는", KeywordCategory.ATMOSPHERE),
    MOUNTAIN_VIEW("산이 보이는", KeywordCategory.ATMOSPHERE),
    LOCAL("로컬", KeywordCategory.ATMOSPHERE),
    QUIET("한적한", KeywordCategory.ATMOSPHERE),
    CROWDED("붐비는", KeywordCategory.ATMOSPHERE),
    LOCAL_POPULAR("현지인이 많은", KeywordCategory.ATMOSPHERE),
    COZY("아늑한", KeywordCategory.ATMOSPHERE),
    TRENDY("트렌디한", KeywordCategory.ATMOSPHERE);

    private final String displayName;
    private final KeywordCategory category;
}
