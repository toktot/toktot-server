package com.toktot.domain.review.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeywordType {
    // 분위기 관련 (ATMOSPHERE)
    DATE_RECOMMENDED("데이트추천", KeywordCategory.ATMOSPHERE),
    QUIET_ATMOSPHERE("한적한 분위기", KeywordCategory.ATMOSPHERE),
    FAMILY_DINING("가족 외식", KeywordCategory.ATMOSPHERE),
    LIVELY("시끌벅적", KeywordCategory.ATMOSPHERE),
    PHOTO_SPOT("사진 맛집", KeywordCategory.ATMOSPHERE),
    SOLO_DINING("혼밥 가능", KeywordCategory.ATMOSPHERE),

    // 음식 관련 (FOOD)
    FRESH("신선해요", KeywordCategory.FOOD),
    GENEROUS_PORTIONS("양이 많아요", KeywordCategory.FOOD),
    GOOD_VALUE("가성비 좋아요", KeywordCategory.FOOD),
    UNIQUE("특이해요", KeywordCategory.FOOD),
    NEAT("정갈해요", KeywordCategory.FOOD),
    DELICIOUS("맛있어요", KeywordCategory.FOOD),

    // 청결 관련 (CLEANLINESS)
    CLEAN_STORE("매장이 청결해요", KeywordCategory.CLEANLINESS),
    CLEAN_BATHROOM("화장실이 깨끗해요", KeywordCategory.CLEANLINESS),
    CLEAN_TABLE("깨끗한 테이블", KeywordCategory.CLEANLINESS),

    // 가격 관련 (PRICE)
    NO_PRICE_INFO("가격 미기재", KeywordCategory.PRICE),
    NO_OVERCHARGING("바가지 없음", KeywordCategory.PRICE),
    REASONABLE("적당해요", KeywordCategory.PRICE),
    GOOD_VALUE_PRICE("가성비 좋음", KeywordCategory.PRICE),
    REASONABLE_CONSUMPTION("합리적 소비", KeywordCategory.PRICE),

    // 서비스 관련 (SERVICE)
    FAST_SERVICE("응대가 빨라요", KeywordCategory.SERVICE),
    LONG_WAIT("대기가 길어요", KeywordCategory.SERVICE),
    RESERVATION_REQUIRED("예약 필수", KeywordCategory.SERVICE),
    FRIENDLY("친절해요", KeywordCategory.SERVICE),
    WAITING_AREA("대기공간 있어요", KeywordCategory.SERVICE),
    PET_FRIENDLY("애견동반", KeywordCategory.SERVICE),

    // 접근성 관련 (ACCESSIBILITY)
    PARKING_AVAILABLE("주차장 있어요", KeywordCategory.ACCESSIBILITY),
    EASY_TO_FIND("찾기 쉬워요", KeywordCategory.ACCESSIBILITY),
    PUBLIC_TRANSPORT("대중교통 편리", KeywordCategory.ACCESSIBILITY);

    private final String displayName;
    private final KeywordCategory category;
}
