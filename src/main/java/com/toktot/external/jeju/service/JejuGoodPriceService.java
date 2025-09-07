package com.toktot.external.jeju.service;

import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.restaurant.type.DataSource;
import com.toktot.external.jeju.client.JejuGoodPriceClient;
import com.toktot.external.jeju.dto.JejuGoodPriceItem;
import com.toktot.external.jeju.dto.JejuGoodPriceProcessResult;
import com.toktot.external.kakao.dto.response.KakaoPlaceInfo;
import com.toktot.external.kakao.dto.response.KakaoPlaceSearchResponse;
import com.toktot.external.kakao.service.KakaoMapService;
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
public class JejuGoodPriceService {

    private final JejuGoodPriceClient jejuGoodPriceClient;
    private final KakaoMapService kakaoMapService;
    private final RestaurantRepository restaurantRepository;

    public int syncAllJejuGoodPriceStores() {
        log.info("제주시 착한가격업소 배치 시작");

        List<JejuGoodPriceItem> items = jejuGoodPriceClient.getAllGoodPriceStores();

        if (items.isEmpty()) {
            log.warn("제주시 착한가격업소 데이터가 없습니다.");
            return 0;
        }

        JejuGoodPriceProcessResult result = JejuGoodPriceProcessResult.create();
        log.info("제주시 착한가격업소 {} 개 처리 시작", items.size());

        for (JejuGoodPriceItem item : items) {
            result = result.incrementTotal();

            if (!item.isValidForProcessing()) {
                String skipReason = getSkipReason(item);
                log.debug("항목 스킵 - {}: {} ({})", skipReason, item.getCleanBusinessName(), item.getCleanIndustryType());
                result = result.incrementSkip();
                continue;
            }

            try {
                if (processJejuGoodPriceItemInNewTransaction(item)) {
                    result = result.incrementSuccess();
                } else {
                    result = result.incrementSkip();
                }
            } catch (Exception e) {
                log.error("제주시 착한가격업소 처리 중 오류 발생: businessName={}",
                        item.getCleanBusinessName(), e);
                result = result.incrementError();
            }
        }

        log.info("제주시 착한가격업소 배치 완료 - 총: {}, 성공: {}, 스킵: {}, 오류: {}",
                result.totalCount(), result.successCount(), result.skipCount(), result.errorCount());

        return result.successCount();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processJejuGoodPriceItemInNewTransaction(JejuGoodPriceItem item) {
        return processJejuGoodPriceItem(item);
    }

    private boolean processJejuGoodPriceItem(JejuGoodPriceItem item) {
        String businessName = item.getCleanBusinessName();
        Optional<Restaurant> existingRestaurant = findExistingRestaurant(businessName);

        if (existingRestaurant.isPresent()) {
            return updateExistingRestaurant(existingRestaurant.get(), item);
        } else {
            return createNewRestaurantFromKakao(item);
        }
    }

    private Optional<Restaurant> findExistingRestaurant(String businessName) {
        Optional<Restaurant> exactMatch = restaurantRepository.findByNameAndIsActive(businessName, true);
        if (exactMatch.isPresent()) {
            log.debug("정확한 매장명 매칭: {}", businessName);
            return exactMatch;
        }

        List<Restaurant> similarRestaurants = restaurantRepository.findByNameContainingIgnoreCaseAndIsActive(businessName, true);
        if (!similarRestaurants.isEmpty()) {
            log.debug("유사한 매장명 발견: {} -> {}", businessName, similarRestaurants.get(0).getName());
            return Optional.of(similarRestaurants.get(0));
        }

        return Optional.empty();
    }

    private boolean updateExistingRestaurant(Restaurant restaurant, JejuGoodPriceItem item) {
        boolean isAlreadyJejuGoodPriceStore = restaurant.getIsGoodPriceStore() &&
                                              DataSource.JEJU_GOOD_PRICE.equals(restaurant.getDataSource());

        if (isAlreadyJejuGoodPriceStore) {
            if (isMenuInfoDifferent(restaurant, item.getCleanMenuInfo())) {
                restaurant.setPopularMenus(item.getCleanMenuInfo());
                restaurantRepository.save(restaurant);
                log.debug("기존 착한가격업소 메뉴 정보 업데이트: {}", restaurant.getName());
                return true;
            } else {
                log.debug("기존 착한가격업소 스킵 (동일한 메뉴 정보): {}", restaurant.getName());
                return false;
            }
        }

        restaurant.updateJejuGoodPriceInfo(item.getCleanMenuInfo());
        restaurantRepository.save(restaurant);
        log.info("기존 매장을 착한가격업소로 업데이트: {}", restaurant.getName());
        return true;
    }

    private boolean createNewRestaurantFromKakao(JejuGoodPriceItem item) {
        try {
            KakaoPlaceSearchResponse kakaoResponse = kakaoMapService.searchRestaurantByName(item.getCleanBusinessName());

            if (kakaoResponse == null || kakaoResponse.placeInfos() == null || kakaoResponse.placeInfos().isEmpty()) {
                log.debug("카카오에서 매장을 찾을 수 없음: {}", item.getCleanBusinessName());
                return false;
            }

            KakaoPlaceInfo kakaoPlaceInfo = kakaoResponse.placeInfos().get(0);

            Optional<Restaurant> existingByKakaoId = restaurantRepository.findByExternalKakaoIdAndIsActive(
                    kakaoPlaceInfo.getId(), true);

            if (existingByKakaoId.isPresent()) {
                return updateExistingRestaurant(existingByKakaoId.get(), item);
            }

            Restaurant newRestaurant = kakaoPlaceInfo.toEntity();
            newRestaurant.updateJejuGoodPriceInfo(item.getCleanMenuInfo());

            if (!newRestaurant.hasRequiredFields()) {
                log.debug("필수 필드 누락으로 매장 생성 실패: {}", item.getCleanBusinessName());
                return false;
            }

            restaurantRepository.save(newRestaurant);
            log.info("새로운 착한가격업소 매장 생성: {}", newRestaurant.getName());
            return true;

        } catch (Exception e) {
            log.error("카카오 검색 및 매장 생성 중 오류 발생: businessName={}",
                    item.getCleanBusinessName(), e);
            return false;
        }
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

    private String getSkipReason(JejuGoodPriceItem item) {
        if (!item.hasValidBusinessName()) {
            return "사업장명 없음";
        }
        if (!item.hasMenuInfo()) {
            return "메뉴 정보 없음";
        }
        if (!item.isValidIndustryType()) {
            return "업종 필터링 (" + item.getCleanIndustryType() + ")";
        }
        if (!item.hasValidCoordinates()) {
            return "좌표 유효성 검사 실패";
        }
        return "알 수 없는 이유";
    }
}
