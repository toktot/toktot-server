package com.toktot.external.seogwipo.service;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
import com.toktot.external.seogwipo.client.SeogwipoGoodPriceClient;
import com.toktot.external.seogwipo.dto.SeogwipoGoodPriceItem;
import com.toktot.external.seogwipo.dto.SeogwipoGoodPriceProcessResult;
import com.toktot.external.seogwipo.dto.SeogwipoGoodPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeogwipoGoodPriceService {

    private final SeogwipoGoodPriceClient seogwipoGoodPriceClient;
    private final KakaoMapService kakaoMapService;
    private final RestaurantRepository restaurantRepository;

    public int syncAllSeogwipoGoodPriceStores() {
        log.info("서귀포 착한가격업소 배치 시작");

        SeogwipoGoodPriceResponse response = seogwipoGoodPriceClient.getAllGoodPriceStores();

        if (response == null || response.getItems() == null) {
            log.warn("서귀포 착한가격업소 데이터가 없습니다.");
            return 0;
        }

        List<SeogwipoGoodPriceItem> items = response.getItems();
        SeogwipoGoodPriceProcessResult result = SeogwipoGoodPriceProcessResult.create();

        log.info("서귀포 착한가격업소 {} 개 처리 시작", items.size());

        for (SeogwipoGoodPriceItem item : items) {
            result = result.incrementTotal();

            if (!item.hasValidShopTitle()) {
                log.debug("가게명이 없는 항목 스킵: {}", item.getShopCode());
                result = result.incrementSkip();
                continue;
            }

            if (!item.hasMenuInfo()) {
                log.debug("메뉴 정보가 없는 항목 스킵: {}", item.getShopTitle());
                result = result.incrementSkip();
                continue;
            }

            try {
                if (processSeogwipoGoodPriceItemInNewTransaction(item)) {
                    result = result.incrementSuccess();
                } else {
                    result = result.incrementSkip();
                }
            } catch (Exception e) {
                log.error("서귀포 착한가격업소 처리 중 오류 발생: shopTitle={}", item.getShopTitle(), e);
                result = result.incrementError();
            }
        }

        log.info("서귀포 착한가격업소 배치 완료 - 총: {}, 성공: {}, 스킵: {}, 오류: {}",
                result.totalCount(), result.successCount(), result.skipCount(), result.errorCount());

        return result.successCount();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processSeogwipoGoodPriceItemInNewTransaction(SeogwipoGoodPriceItem item) {
        return processSeogwipoGoodPriceItem(item);
    }

    private boolean processSeogwipoGoodPriceItem(SeogwipoGoodPriceItem item) {
        Optional<Restaurant> existingRestaurant = findExistingRestaurant(item.getShopTitle());

        if (existingRestaurant.isPresent()) {
            return updateExistingRestaurant(existingRestaurant.get(), item);
        } else {
            return createNewRestaurantFromKakao(item);
        }
    }

    private Optional<Restaurant> findExistingRestaurant(String shopTitle) {
        Optional<Restaurant> exactMatch = restaurantRepository.findByNameAndIsActive(shopTitle, true);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        List<Restaurant> similarRestaurants = restaurantRepository.findByNameContainingIgnoreCaseAndIsActive(shopTitle, true);
        if (!similarRestaurants.isEmpty()) {
            log.debug("유사한 매장명 발견: {} -> {}", shopTitle, similarRestaurants.get(0).getName());
            return Optional.of(similarRestaurants.get(0));
        }

        return Optional.empty();
    }

    private boolean updateExistingRestaurant(Restaurant restaurant, SeogwipoGoodPriceItem item) {
        if (shouldSkipUpdate(restaurant)) {
            if (isMenuInfoDifferent(restaurant, item.getShopGoods())) {
                restaurant.setPopularMenus(item.getShopGoods());
                log.debug("기존 착한가격업소 메뉴 정보 업데이트: {}", restaurant.getName());
                return true;
            } else {
                log.debug("기존 착한가격업소 스킵 (동일한 메뉴 정보): {}", restaurant.getName());
                return false;
            }
        }

        restaurant.updateSeogwipoGoodPriceInfo(item.getShopGoods());
        log.info("기존 매장을 착한가격업소로 업데이트: {}", restaurant.getName());
        return true;
    }

    private boolean createNewRestaurantFromKakao(SeogwipoGoodPriceItem item) {
        try {
            KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchRestaurantByName(item.getShopTitle());

            if (kakaoResponse == null || kakaoResponse.placeInfos() == null || kakaoResponse.placeInfos().isEmpty()) {
                log.debug("카카오에서 매장을 찾을 수 없음: {}", item.getShopTitle());
                return false;
            }

            KakaoPlaceInfo kakaoPlaceInfo = kakaoResponse.placeInfos().get(0);

            Optional<Restaurant> existingByKakaoId = restaurantRepository.findByExternalKakaoIdAndIsActive(
                    kakaoPlaceInfo.getId(), true);

            if (existingByKakaoId.isPresent()) {
                return updateExistingRestaurant(existingByKakaoId.get(), item);
            }

            Restaurant newRestaurant = createRestaurantFromKakaoAndSeogwipo(kakaoPlaceInfo, item);

            if (newRestaurant.getName() == null || newRestaurant.getLatitude() == null || newRestaurant.getLongitude() == null) {
                log.warn("카카오 데이터에 필수 필드 누락: {}", item.getShopTitle());
                return false;
            }

            Restaurant savedRestaurant = restaurantRepository.save(newRestaurant);

            log.info("새 착한가격업소 생성: {} (ID: {}, 카카오 ID: {})",
                    savedRestaurant.getName(), savedRestaurant.getId(), kakaoPlaceInfo.getId());
            return true;

        } catch (Exception e) {
            log.error("카카오 검색 및 매장 생성 중 오류: {}", item.getShopTitle(), e);
            return false;
        }
    }

    private Restaurant createRestaurantFromKakaoAndSeogwipo(KakaoPlaceInfo kakaoPlaceInfo, SeogwipoGoodPriceItem item) {
        Restaurant restaurant = kakaoPlaceInfo.toEntity();
        restaurant.updateSeogwipoGoodPriceInfo(item.getShopGoods());

        return restaurant;
    }

    private boolean shouldSkipUpdate(Restaurant restaurant) {
        return DataSource.SEOGWIPO_GOOD_PRICE.equals(restaurant.getDataSource())
               && Boolean.TRUE.equals(restaurant.getIsGoodPriceStore());
    }

    private boolean isMenuInfoDifferent(Restaurant restaurant, String newMenuInfo) {
        String currentMenuInfo = restaurant.getPopularMenus();

        if (currentMenuInfo == null && newMenuInfo == null) {
            return false;
        }

        if (currentMenuInfo == null || newMenuInfo == null) {
            return true;
        }

        return !currentMenuInfo.trim().equals(newMenuInfo.trim());
    }
}
