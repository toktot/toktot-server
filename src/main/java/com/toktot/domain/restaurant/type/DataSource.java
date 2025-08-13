package com.toktot.domain.restaurant.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataSource {

    TOUR_API("TourAPI"),
    KAKAO("Kakao Map"),
    USER_CREATED("User Created"),

    ;
    private final String description;
}
