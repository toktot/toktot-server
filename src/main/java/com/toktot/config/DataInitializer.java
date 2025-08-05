package com.toktot.config;

import com.toktot.domain.localfood.LocalFood;
import com.toktot.domain.localfood.LocalFoodType;
import com.toktot.domain.localfood.repository.LocalFoodRepository;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.user.User;
import com.toktot.domain.user.UserAgreement;
import com.toktot.domain.user.UserProfile;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.domain.user.type.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final LocalFoodRepository localFoodRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== 똑똣 초기 데이터 삽입 시작 ===");

        insertLocalFoodData();
        insertRestaurantData();
        insertTestUser();

        log.info("=== 똑똣 초기 데이터 삽입 완료 ===");
    }

    private void insertLocalFoodData() {
        // 이미 데이터가 있으면 건너뛰기
        if (localFoodRepository.count() > 0) {
            log.info("🔄 향토음식 데이터가 이미 {}개 존재합니다. 삽입을 건너뜁니다.",
                    localFoodRepository.count());
            return;
        }

        log.info("🍽️ 향토음식 데이터 삽입 중...");

        // LocalFoodType enum의 모든 값을 순서대로 가져와서 LocalFood 엔티티 생성
        LocalFoodType[] foodTypes = LocalFoodType.values();

        List<LocalFood> localFoods = IntStream.range(0, foodTypes.length)
                .mapToObj(i -> LocalFood.builder()
                        .localFoodType(foodTypes[i])
                        .displayOrder(i + 1) // 1부터 시작하는 순서
                        .isActive(true)
                        .build())
                .toList();

        localFoodRepository.saveAll(localFoods);

        log.info("🎉 향토음식 데이터 {}개 삽입 완료!", localFoods.size());

        // 삽입된 데이터 로그 출력
        localFoods.forEach(food ->
                log.debug("📋 향토음식 등록: {} ({}), 순서: {}",
                        food.getLocalFoodType().getDisplayName(),
                        food.getLocalFoodType().getIconName(),
                        food.getDisplayOrder())
        );
    }

    private void insertRestaurantData() {
        // 이미 데이터가 있으면 건너뛰기
        if (restaurantRepository.count() > 0) {
            log.info("🔄 가게 데이터가 이미 {}개 존재합니다. 삽입을 건너뜁니다.",
                    restaurantRepository.count());
            return;
        }

        log.info("📍 가게 데이터 삽입 중...");

        List<Restaurant> restaurants = List.of(
                createRestaurant("올레국수 본점", "kakao_001", "tour_001"),
                createRestaurant("흑돼지마을 제주점", "kakao_002", "tour_002"),
                createRestaurant("제주맥주 본점", "kakao_003", "tour_003"),
                createRestaurant("협재해수욕장맛집", "kakao_004", "tour_004"),
                createRestaurant("제주 전통시장 국수집", "kakao_005", "tour_005"),
                createRestaurant("중문단지 맛집", "kakao_006", "tour_006"),
                createRestaurant("성산일출봉 해물탕", "kakao_007", "tour_007"),
                createRestaurant("정방폭포 횟집", "kakao_008", "tour_008"),
                createRestaurant("천지연폭포 근처 카페", "kakao_009", "tour_009"),
                createRestaurant("우도 땅콩아이스크림", "kakao_010", "tour_010"),
                createRestaurant("제주 흑돼지 전문점", "kakao_011", "tour_011"),
                createRestaurant("한라산 등반 맛집", "kakao_012", "tour_012"),
                createRestaurant("애월 카페거리", "kakao_013", "tour_013"),
                createRestaurant("서귀포 매일올레시장 맛집", "kakao_014", "tour_014"),
                createRestaurant("제주공항 근처 국수집", "kakao_015", "tour_015")
        );

        restaurantRepository.saveAll(restaurants);

        log.info("🎉 가게 데이터 {}개 삽입 완료!", restaurants.size());
    }

    private void insertTestUser() {
        // 테스트 사용자가 이미 있으면 건너뛰기
        if (userRepository.existsByEmail("test@toktot.com")) {
            log.info("🔄 테스트 사용자가 이미 존재합니다. 삽입을 건너뜁니다.");
            return;
        }

        log.info("👤 테스트 사용자 삽입 중...");

        // 비밀번호 인코딩 (qwer1234!)
        String encodedPassword = passwordEncoder.encode("qwer1234!");

        // 사용자 생성
        User testUser = User.builder()
                .email("test@toktot.com")
                .password(encodedPassword)
                .authProvider(AuthProvider.EMAIL)
                .nickname("테스트사용자")
                .build();

        // 사용자 프로필 생성
        UserProfile userProfile = UserProfile.createDefault(testUser);
        testUser.assignUserProfile(userProfile);

        // 사용자 약관 동의 생성
        UserAgreement userAgreement = UserAgreement.createWithFullAgreement(
                testUser, "127.0.0.1", "Spring-Boot-DataInitializer");
        testUser.assignUserAgreement(userAgreement);

        // 저장
        userRepository.save(testUser);

        log.info("🎉 테스트 사용자 생성 완료! 이메일: test@toktot.com, 비밀번호: qwer1234!");
    }

    private Restaurant createRestaurant(String name, String kakaoId, String tourId) {
        return Restaurant.builder()
                .name(name)
                .externalKakaoId(kakaoId)
                .externalTourApiId(tourId)
                .build();
    }
}
