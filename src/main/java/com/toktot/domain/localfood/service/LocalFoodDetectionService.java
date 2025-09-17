package com.toktot.domain.localfood.service;

import com.toktot.domain.localfood.LocalFoodType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFoodDetectionService {

    @Cacheable(value = "localFoodDetection", key = "#menuName", unless = "#result.isEmpty()")
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
}
