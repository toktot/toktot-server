package com.toktot.domain.localfood;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocalFoodType {

    DOMBE_MEAT("돔베고기", "dombe_meat_icon"),
    MEAT_NOODLE_SOUP("고기국수", "meat_noodle_icon"),
    SEA_URCHIN_SEAWEED_SOUP("성게미역국", "sea_urchin_seaweed_icon"),
    BRACKEN_HANGOVER_SOUP("고사리 해장국", "bracken_hangover_icon"),
    GRILLED_RED_TILEFISH("옥돔구이", "red_tilefish_icon"),
    GRILLED_CUTLASSFISH("갈치구이", "cutlassfish_icon"),
    RAW_FISH_MULHOE("회/물회", "raw_fish_icon"),
    BING_RICE_CAKE("빙떡", "bing_icon"),
    OMEGI_RICE_CAKE("오메기떡", "omegi_icon"),


    ;

    private final String displayName;
    private final String iconName;

    @JsonCreator
    public static LocalFoodType from(String value) {
        try {
            return LocalFoodType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "local_food type error");
        }
    }

}
