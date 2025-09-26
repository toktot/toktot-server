package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.restaurant.Restaurant;
import com.toktot.domain.restaurant.RestaurantMenuSubmissionImage;
import com.toktot.domain.restaurant.RestaurantMenuSubmissions;
import com.toktot.domain.restaurant.repository.RestaurantMenuSubmissionsRepository;
import com.toktot.domain.restaurant.repository.RestaurantRepository;
import com.toktot.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuSubmissionService {

    private final RestaurantMenuSubmissionsRepository restaurantMenuSubmissionsRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuImageS3StorageService menuImageS3StorageService;

    @Transactional
    public void saveMenuSubmissions(Long restaurantId, MultipartFile[] files, User user) {
        log.info("메뉴 제출 저장 시작 - userId: {}, restaurantId: {}, fileCount: {}",
                user.getId(), restaurantId, files.length);

        menuImageS3StorageService.validateMultipleFiles(files);

        Restaurant restaurant = findRestaurant(restaurantId);
        RestaurantMenuSubmissions submission = RestaurantMenuSubmissions.create(user, restaurant);

        for (MultipartFile file : files) {
            String imageUrl = menuImageS3StorageService.uploadImageAndGetImageUrl(file, restaurantId);
            RestaurantMenuSubmissionImage image = RestaurantMenuSubmissionImage.create(imageUrl);
            submission.addImage(image);
        }

        restaurantMenuSubmissionsRepository.save(submission);

        log.info("메뉴 제출 저장 완료 - userId: {}, restaurantId: {}, submissionId: {}",
                user.getId(), restaurantId, submission.getId());
    }

    private Restaurant findRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESTAURANT_NOT_FOUND));
    }
}
