package com.toktot.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PriceParserUtil {

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*)(?:원)?");

    public static Double calculateAveragePrice(String menuString) {
        if (menuString == null || menuString.trim().isEmpty()) {
            return null;
        }

        List<Integer> prices = extractPrices(menuString);

        if (prices.isEmpty()) {
            log.debug("가격 추출 실패: {}", menuString);
            return null;
        }

        OptionalDouble average = prices.stream()
                .mapToInt(Integer::intValue)
                .average();

        if (average.isPresent()) {
            double avgPrice = average.getAsDouble();
            log.debug("가격 파싱 성공: {} -> 평균: {}", menuString, avgPrice);
            return avgPrice;
        }

        return null;
    }

    public static List<Integer> extractPrices(String menuString) {
        List<Integer> prices = new ArrayList<>();

        if (menuString == null || menuString.trim().isEmpty()) {
            return prices;
        }

        Matcher matcher = PRICE_PATTERN.matcher(menuString);

        while (matcher.find()) {
            String priceStr = matcher.group(1);
            try {
                String cleanPrice = priceStr.replace(",", "");
                int price = Integer.parseInt(cleanPrice);

                if (price >= 100 && price <= 1_000_000) {
                    prices.add(price);
                }
            } catch (NumberFormatException e) {
                log.debug("가격 파싱 실패: {}", priceStr);
            }
        }

        return prices;
    }

    public static Integer determinePriceRange(Double averagePrice) {
        if (averagePrice == null) {
            return null;
        }

        int price = averagePrice.intValue();

        if (price <= 9_999) {
            return 0; // 1만원 이하
        } else if (price <= 19_999) {
            return 10_000; // 1만원대
        } else if (price <= 29_999) {
            return 20_000; // 2~3만원대
        } else if (price <= 49_999) {
            return 30_000; // 3~5만원대
        } else if (price <= 69_999) {
            return 50_000; // 5~7만원대
        } else {
            return 70_000; // 7만원 이상
        }
    }

    public static String getPriceRangeName(Integer priceRange) {
        if (priceRange == null) {
            return "미분류";
        }

        return switch (priceRange) {
            case 0 -> "1만원 이하";
            case 10_000 -> "1만원대";
            case 20_000 -> "2~3만원대";
            case 30_000 -> "3~5만원대";
            case 50_000 -> "5~7만원대";
            case 70_000 -> "7만원 이상";
            default -> "미분류";
        };
    }

    public static boolean isValidPriceRange(Integer priceRange) {
        if (priceRange == null) {
            return false;
        }

        return priceRange == 0 ||
                priceRange == 10_000 ||
                priceRange == 20_000 ||
                priceRange == 30_000 ||
                priceRange == 50_000 ||
                priceRange == 70_000;
    }
}
