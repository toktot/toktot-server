package com.toktot.web.dto.request;

public record LocationFilterRequest(
        Double latitude,
        Double longitude,
        Integer radius
) {
    public boolean isValid() {
        boolean allPresent = longitude != null && latitude != null && radius != null;
        boolean allNull = longitude == null && latitude == null && radius == null;

        return (allPresent || allNull) && validateRadius();
    }

    private boolean validateRadius() {
        if (radius == null) {
            return true;
        }

        return radius > 0 && radius <= 3000;
    }

    public boolean isWithinJejuBounds() {
        return latitude >= 33.0 && latitude <= 34.0 &&
                longitude >= 126.0 && longitude <= 127.0;
    }
}
