package com.toktot.external.tourapi.mapper;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.tourapi.dto.TourApiRestaurant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
public class TourApiMapper {

    public Restaurant toRestaurant(TourApiRestaurant dto) {
        if (dto == null) {
            log.warn("TourApiRestaurant DTO가 null입니다.");
            return null;
        }

        if (!isValidRestaurant(dto)) {
            log.debug("필수 필드가 누락된 매장 데이터 스킵: contentId={}, title={}",
                    dto.contentId(), dto.title());
            return null;
        }

        BigDecimal latitude = parseCoordinate(dto.latitude(), "latitude");
        BigDecimal longitude = parseCoordinate(dto.longitude(), "longitude");

        if (latitude == null || longitude == null) {
            log.debug("유효하지 않은 좌표 데이터 스킵: contentId={}, lat={}, lng={}",
                    dto.contentId(), dto.latitude(), dto.longitude());
            return null;
        }

        return Restaurant.builder()
                .name(cleanString(dto.title()))
                .category(mapCategory(dto.category2(), dto.category3()))
                .address(normalizeAddress(dto.address1(), dto.address2()))
                .latitude(latitude)
                .longitude(longitude)
                .phone(cleanPhoneNumber(dto.phoneNumber()))
                .dataSource(DataSource.TOUR_API)
                .externalTourApiId(dto.contentId())
                .isActive(true)
                .isGoodPriceStore(false)
                .searchCount(0)
                .lastSyncedAt(LocalDateTime.now())
                .build();
    }

    private boolean isValidRestaurant(TourApiRestaurant dto) {
        return StringUtils.hasText(dto.contentId()) &&
                StringUtils.hasText(dto.title()) &&
                StringUtils.hasText(dto.latitude()) &&
                StringUtils.hasText(dto.longitude());
    }

    private BigDecimal parseCoordinate(String coordinate, String type) {
        if (!StringUtils.hasText(coordinate)) {
            return null;
        }

        try {
            BigDecimal value = new BigDecimal(coordinate);

            if ("latitude".equals(type)) {
                if (value.compareTo(new BigDecimal("33.0")) < 0 ||
                        value.compareTo(new BigDecimal("33.6")) > 0) {
                    log.debug("제주도 범위를 벗어난 위도: {}", value);
                    return null;
                }
            } else if ("longitude".equals(type)) {
                if (value.compareTo(new BigDecimal("126.1")) < 0 ||
                        value.compareTo(new BigDecimal("127.0")) > 0) {
                    log.debug("제주도 범위를 벗어난 경도: {}", value);
                    return null;
                }
            }

            return value;
        } catch (NumberFormatException e) {
            log.debug("좌표 변환 실패: {} = {}", type, coordinate);
            return null;
        }
    }

    private String normalizeAddress(String addr1, String addr2) {
        StringBuilder address = new StringBuilder();

        if (StringUtils.hasText(addr1)) {
            String normalized = addr1.replace("제주특별자치도", "제주도").trim();
            address.append(normalized);
        }

        if (StringUtils.hasText(addr2)) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(addr2.trim());
        }

        return address.length() > 0 ? address.toString() : null;
    }

    private String mapCategory(String cat2, String cat3) {
        if ("A0502".equals(cat2)) {
            return mapRestaurantSubCategory(cat3);
        } else if ("A0503".equals(cat2)) {
            return "카페";
        }

        return "음식점";
    }

    private String mapRestaurantSubCategory(String cat3) {
        if (!StringUtils.hasText(cat3)) {
            return "음식점";
        }

        return switch (cat3) {
            case "A05020100" -> "한식";
            case "A05020200" -> "서양식";
            case "A05020300" -> "일식";
            case "A05020400" -> "중식";
            case "A05020500" -> "아시아식";
            case "A05020600" -> "패밀리레스토랑";
            case "A05020700" -> "패스트푸드";
            case "A05020900" -> "기타";
            default -> "음식점";
        };
    }

    private String cleanPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        String cleaned = phone.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("064") && cleaned.length() >= 9) {
            return cleaned.substring(0, 3) + "-" +
                    cleaned.substring(3, cleaned.length() - 4) + "-" +
                    cleaned.substring(cleaned.length() - 4);
        }

        return cleaned.length() > 0 ? cleaned : null;
    }

    private String cleanString(String str) {
        return StringUtils.hasText(str) ? str.trim() : null;
    }
}
