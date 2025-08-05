package com.toktot.web.dto.localfood;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.localfood.LocalFood;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocalFoodResponse(
        Long id,

        String name,

        @JsonProperty(value = "icon_name")
        String iconName,

        @JsonProperty(value = "display_order")
        Integer displayOrder
) {

    public static LocalFoodResponse from(LocalFood localFood) {
        return LocalFoodResponse
                .builder()
                .id(localFood.getId())
                .name(localFood.getLocalFoodType().getDisplayName())
                .iconName(localFood.getLocalFoodType().getIconName())
                .displayOrder(localFood.getDisplayOrder())
                .build();
    }
}
