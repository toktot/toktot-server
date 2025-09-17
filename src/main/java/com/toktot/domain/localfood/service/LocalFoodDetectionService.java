package com.toktot.domain.localfood.service;

import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.review.Tooltip;
import com.toktot.domain.review.repository.TooltipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFoodDetectionService {

    private final TooltipRepository tooltipRepository;

    public Optional<LocalFoodType> detectFromMenuName(String menuName) {
        if (menuName == null || menuName.trim().isEmpty()) {
            return Optional.empty();
        }

        log.debug("향토음식 감지 시작 - 메뉴명: {}", menuName);

        Optional<LocalFoodType> result = LocalFoodType.findByMenuName(menuName);

        if (result.isPresent()) {
            log.info("향토음식 감지 성공 - 메뉴명: {}, 타입: {}", menuName, result.get().getDisplayName());
        } else {
            log.debug("향토음식 감지 실패 - 메뉴명: {}", menuName);
        }

        return result;
    }

    public boolean isLocalFood(String menuName) {
        return detectFromMenuName(menuName).isPresent();
    }

    public List<Tooltip> findTooltipsByTypeAndPrice(LocalFoodType localFoodType,
                                                    Integer minPrice, Integer maxPrice) {
        List<Tooltip> tooltipsInPriceRange = tooltipRepository.findFoodTooltipsByPriceRange(minPrice, maxPrice);

        return tooltipsInPriceRange.stream()
                .filter(tooltip -> {
                    String menuName = tooltip.getMenuName();
                    return menuName != null &&
                            detectFromMenuName(menuName)
                                    .map(type -> type.equals(localFoodType))
                                    .orElse(false);
                })
                .collect(Collectors.toList());
    }

    public List<Tooltip> findTooltipsByType(LocalFoodType localFoodType) {
        List<Tooltip> allTooltips = tooltipRepository.findAllFoodTooltipsWithPriceData();

        return allTooltips.stream()
                .filter(tooltip -> {
                    String menuName = tooltip.getMenuName();
                    return menuName != null &&
                            detectFromMenuName(menuName)
                                    .map(type -> type.equals(localFoodType))
                                    .orElse(false);
                })
                .collect(Collectors.toList());
    }
}
