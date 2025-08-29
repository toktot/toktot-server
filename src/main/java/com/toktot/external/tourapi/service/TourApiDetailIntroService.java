package com.toktot.external.tourapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.tourapi.dto.TourApiDetailIntro;
import com.toktot.external.tourapi.dto.TourApiResponse;
import com.toktot.external.tourapi.mapper.TourApiDetailIntroWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiDetailIntroService {

    private final TourApiClient tourApiClient;
    private final RestaurantRepository restaurantRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private void updateFields(TourApiDetailIntro detailIntro, Restaurant restaurant) {
        try {
            if (detailIntro.openTimeFood() != null && !detailIntro.openTimeFood().trim().isEmpty()) {
                String cleanOpenTime = cleanHtmlTags(detailIntro.openTimeFood().trim());
                String cleanRestDay = detailIntro.restDateFood() != null ?
                        cleanHtmlTags(detailIntro.restDateFood().trim()) : null;

                String businessHoursJson = createBusinessHoursJson(cleanOpenTime, cleanRestDay);
                restaurant.setBusinessHours(businessHoursJson);
                log.debug("영업시간 설정: {}", businessHoursJson);
            }

            if (detailIntro.firstMenu() != null || detailIntro.treatMenu() != null) {
                String firstMenu = detailIntro.firstMenu() != null ?
                        detailIntro.firstMenu().trim() : null;
                String treatMenu = detailIntro.treatMenu() != null ?
                        cleanMenuText(detailIntro.treatMenu().trim()) : null;

                String menusJson = createMenusJson(firstMenu, treatMenu);
                restaurant.setPopularMenus(menusJson);
                log.debug("메뉴 설정: {}", menusJson);
            }

            if (detailIntro.infoCenterFood() != null && !detailIntro.infoCenterFood().trim().isEmpty()) {
                String currentPhone = restaurant.getPhone();
                if (currentPhone == null || currentPhone.trim().isEmpty()) {
                    restaurant.setPhone(detailIntro.infoCenterFood().trim());
                    log.debug("전화번호 설정: {}", detailIntro.infoCenterFood().trim());
                }
            }

        } catch (Exception e) {
            log.error("필드 업데이트 실패", e);
        }
    }

    private String cleanHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<br>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("※\\s*", "")
                .trim();
    }

    private String cleanMenuText(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s*/\\s*", ", ")
                .replaceAll("\\s+등$", " 등")
                .trim();
    }

    private String createBusinessHoursJson(String openTime, String restDay) {
        try {
            Map<String, String> hours = new HashMap<>();
            if (openTime != null) hours.put("openTime", openTime);
            if (restDay != null) hours.put("restDay", restDay);
            return objectMapper.writeValueAsString(hours);
        } catch (Exception e) {
            log.warn("영업시간 JSON 생성 실패", e);
            return null;
        }
    }

    private String createMenusJson(String firstMenu, String treatMenu) {
        try {
            Map<String, String> menus = new HashMap<>();
            if (firstMenu != null) menus.put("firstMenu", firstMenu);
            if (treatMenu != null) menus.put("treatMenu", treatMenu);
            return objectMapper.writeValueAsString(menus);
        } catch (Exception e) {
            log.warn("메뉴 JSON 생성 실패", e);
            return null;
        }
    }

    @Transactional
    public int syncAllRestaurantsDetailIntro() {
        List<Restaurant> restaurants = restaurantRepository.findAllByDataSourceAndIsActive(DataSource.TOUR_API, true);

        int total = restaurants.size();
        int success = 0;

        log.info("DetailIntro 배치 동기화 시작: {} 개 매장", total);

        for (Restaurant restaurant : restaurants) {
            if (restaurant.getExternalTourApiId() != null) {
                try {
                    if (updateRestaurantDetailIntroInternal(restaurant)) {
                        success++;
                        if (success % 10 == 0) {
                            log.info("진행 상황: {}/{}", success, total);
                        }
                    }
                    Thread.sleep(200);
                } catch (Exception e) {
                    log.error("매장 업데이트 실패: id={}", restaurant.getId(), e);
                }
            } else {
                log.warn("contentId가 null: id={}", restaurant.getId());
            }
        }

        log.info("DetailIntro 배치 동기화 완료: {}/{} 성공", success, total);
        return success;
    }

    private boolean updateRestaurantDetailIntroInternal(Restaurant restaurant) {
        String contentId = restaurant.getExternalTourApiId();

        try {
            Restaurant managedRestaurant = entityManager.find(Restaurant.class, restaurant.getId());
            if (managedRestaurant == null) {
                log.warn("매장을 찾을 수 없습니다: id={}", restaurant.getId());
                return false;
            }

            TourApiResponse<TourApiDetailIntroWrapper> response = tourApiClient.getRestaurantDetailIntro(contentId);

            if (response != null && response.response() != null && response.response().body() != null) {
                TourApiDetailIntro detailIntro = response.response().body().items().getFirstItem();

                if (detailIntro != null) {
                    updateFields(detailIntro, managedRestaurant);
                    return true;
                } else {
                    log.warn("DetailIntro가 null입니다: contentId={}", contentId);
                }
            } else {
                log.warn("응답이 유효하지 않습니다: contentId={}", contentId);
            }

        } catch (Exception e) {
            log.error("업데이트 실패: contentId={}", contentId, e);
        }

        return false;
    }
}
