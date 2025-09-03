package com.toktot.web.dto.request;

import jakarta.validation.constraints.*;

public record LocationFilterRequest(
        @DecimalMin(value = "33.0", message = "위치 설정에 오류가 발생했습니다.")
        @DecimalMax(value = "34.0", message = "위치 설정에 오류가 발생했습니다.")
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @DecimalMin(value = "126.0", message = "위치 설정에 오류가 발생했습니다.")
        @DecimalMax(value = "127.0", message = "위치 설정에 오류가 발생했습니다.")
        @NotNull(message = "경도는 필수입니다.")
        Double longitude,

        @Min(value = 100, message = "검색 반경은 최소 100m입니다.")
        @Max(value = 3000, message = "검색 반경은 최대 3km입니다.")
        @NotNull(message = "검색 반경은 필수입니다.")
        Integer radius
) {
    public boolean isValid() {
        return latitude != null && longitude != null && radius != null &&
                latitude >= 33.0 && latitude <= 34.0 &&
                longitude >= 126.0 && longitude <= 127.0 &&
                radius >= 100 && radius <= 3000;
    }

    public boolean isWithinJejuBounds() {
        return latitude >= 33.0 && latitude <= 34.0 &&
                longitude >= 126.0 && longitude <= 127.0;
    }
}
