package com.toktot.domain.restaurant.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataSource {

    TOUR_API("TourAPI"),
    KAKAO("Kakao Map"),
    SEOGWIPO_GOOD_PRICE("seogwipo good price"),
    JEJU_GOOD_PRICE("jeju good price"),
    USER_CREATED("User Created"),

    ;
    private final String description;
}
