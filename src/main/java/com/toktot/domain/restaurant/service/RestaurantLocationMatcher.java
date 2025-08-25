package com.toktot.domain.restaurant.service;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantLocationMatcher {

    private static final double DEFAULT_MATCH_RADIUS_METERS = 50.0;

    private final RestaurantRepository restaurantRepository;

    public Optional<Restaurant> findExistingRestaurant(String restaurantName, BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        List<Restaurant> nearbyRestaurants = findRestaurantsWithinRadius(latitude, longitude, DEFAULT_MATCH_RADIUS_METERS);

        if (nearbyRestaurants.isEmpty()) {
            return Optional.empty();
        }

        if (restaurantName != null && !restaurantName.trim().isEmpty()) {
            Optional<Restaurant> nameMatch = nearbyRestaurants.stream()
                    .filter(r -> isSimilarName(restaurantName, r.getName()))
                    .min((r1, r2) -> compareDistance(latitude, longitude, r1, r2));

            if (nameMatch.isPresent()) {
                log.debug("매장명+좌표 매칭: {} ≈ {}", restaurantName, nameMatch.get().getName());
                return nameMatch;
            }
        }

        Restaurant closest = nearbyRestaurants.stream()
                .min((r1, r2) -> compareDistance(latitude, longitude, r1, r2))
                .get();

        double distance = calculateDistance(latitude, longitude, closest.getLatitude(), closest.getLongitude());
        log.debug("좌표 매칭: {} (거리: {}m)", closest.getName(), Math.round(distance));

        return Optional.of(closest);
    }

    private List<Restaurant> findRestaurantsWithinRadius(BigDecimal latitude, BigDecimal longitude, double radiusMeters) {
        double[] bounds = calculateBounds(latitude, longitude, radiusMeters);

        List<Restaurant> candidates = restaurantRepository.findByLatitudeBetweenAndLongitudeBetween(
                BigDecimal.valueOf(bounds[0]), BigDecimal.valueOf(bounds[1]),
                BigDecimal.valueOf(bounds[2]), BigDecimal.valueOf(bounds[3])
        );

        return candidates.stream()
                .filter(r -> calculateDistance(latitude, longitude, r.getLatitude(), r.getLongitude()) <= radiusMeters)
                .toList();
    }

    private boolean isSimilarName(String name1, String name2) {
        if (name1 == null || name2 == null) return false;

        String clean1 = name1.replaceAll("\\s+", "").toLowerCase();
        String clean2 = name2.replaceAll("\\s+", "").toLowerCase();

        return clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1);
    }

    private int compareDistance(BigDecimal lat, BigDecimal lon, Restaurant r1, Restaurant r2) {
        double dist1 = calculateDistance(lat, lon, r1.getLatitude(), r1.getLongitude());
        double dist2 = calculateDistance(lat, lon, r2.getLatitude(), r2.getLongitude());
        return Double.compare(dist1, dist2);
    }

    private double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lon1Rad = Math.toRadians(lon1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double lon2Rad = Math.toRadians(lon2.doubleValue());

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371.0 * c * 1000;
    }

    private double[] calculateBounds(BigDecimal centerLat, BigDecimal centerLon, double radiusMeters) {
        double lat = centerLat.doubleValue();
        double lon = centerLon.doubleValue();

        double latOffset = radiusMeters / 111320.0;
        double lonOffset = radiusMeters / (Math.cos(Math.toRadians(lat)) * 111320.0);

        return new double[] {
                lat - latOffset,
                lat + latOffset,
                lon - lonOffset,
                lon + lonOffset
        };
    }
}
