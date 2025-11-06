package com.toktot.domain.restaurant.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MenuCategory {
    MAIN_DISH("메인 요리", 1),
    SIDE_DISH("사이드 메뉴", 2),
    SOUP("국물 요리", 3),
    NOODLE("면 요리", 4),
    RICE("밥 요리", 5),
    SEAFOOD("해산물", 6),
    MEAT("육류", 7),
    LOCAL_FOOD("향토 음식", 8),
    BEVERAGE("음료", 9),
    DESSERT("디저트", 10),
    ETC("기타", 11),

    ;

    private final String displayName;
    private final int sortOrder;
}
