package com.toktot.domain.restaurant.service;

import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.external.kakao.service.KakaoMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantMatchService {

    private final RestaurantRepository restaurantRepository;
    private final KakaoMapService kakaoMapService;

    @Transactional
    public void addExternalKakaoIdInTourApiRestaurant() {
        restaurantRepository.findByExternalKakaoIdIsNull()
                .forEach(restaurant ->
                        kakaoMapService.searchRestaurantAddress(
                                        restaurant.getAddress()
                                )
                                .placeInfos()
                                .stream()
                                .findFirst()
                                .ifPresentOrElse(
                                        restaurant::updateKakaoData,
                                        () -> restaurant.setIsActive(false)
                                )
                );
    }
}
