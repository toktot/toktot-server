package com.toktot.web.controller.review;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import com.toktot.domain.review.service.ReviewImageService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.review.response.ImageUploadResponse;
import com.toktot.web.dto.review.response.SessionInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/reviews/images")
@RequiredArgsConstructor
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Image upload request - userId: {}, restaurantId: {}, fileCount: {}",
                user.getId(), restaurantId, files.length);

        validateUploadRequest(files, restaurantId);

        List<MultipartFile> fileList = Arrays.asList(files);
        List<ReviewImageDTO> uploadedImages = reviewImageService.uploadImages(
                fileList, user.getId(), restaurantId);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), restaurantId);
        ImageUploadResponse response = ImageUploadResponse.from(uploadedImages, session);

        log.info("Image upload completed - userId: {}, restaurantId: {}, uploaded: {}, total: {}",
                user.getId(), restaurantId, uploadedImages.size(), session.getImageCount());

        return ResponseEntity.ok(ApiResponse.success("이미지 업로드가 완료되었습니다.", response));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> deleteImage(
            @PathVariable String imageId,
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Image delete request - userId: {}, restaurantId: {}, imageId: {}",
                user.getId(), restaurantId, imageId);

        validateDeleteRequest(imageId, restaurantId);

        reviewImageService.deleteImage(imageId, user.getId(), restaurantId);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), restaurantId);
        SessionInfoResponse response = SessionInfoResponse.from(session);

        log.info("Image deleted - userId: {}, restaurantId: {}, imageId: {}, remaining: {}",
                user.getId(), restaurantId, imageId, session.getImageCount());

        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SessionInfoResponse>> getCurrentSession(
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Session info request - userId: {}, restaurantId: {}",
                user.getId(), restaurantId);

        validateRestaurantId(restaurantId);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), restaurantId);
        SessionInfoResponse response = SessionInfoResponse.from(session);

        return ResponseEntity.ok(ApiResponse.success("세션 정보를 조회했습니다.", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearSession(
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Session clear request - userId: {}, restaurantId: {}",
                user.getId(), restaurantId);

        validateRestaurantId(restaurantId);

        ReviewSessionDTO sessionBeforeClear = reviewImageService.getCurrentSession(user.getId(), restaurantId);
        int imageCountBeforeClear = sessionBeforeClear.getImageCount();

        reviewImageService.clearSession(user.getId(), restaurantId);

        log.info("Session cleared - userId: {}, restaurantId: {}, clearedCount: {}",
                user.getId(), restaurantId, imageCountBeforeClear);

        return ResponseEntity.ok(ApiResponse.success("세션이 초기화되었습니다.", "cleared"));
    }

    private void validateUploadRequest(MultipartFile[] files, Long restaurantId) {
        if (files == null || files.length == 0) {
            log.debug("Upload validation failed - no files, restaurantId: {}", restaurantId);
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일을 선택해주세요.");
        }

        if (files.length > 5) {
            log.debug("Upload validation failed - too many files: {}, restaurantId: {}",
                    files.length, restaurantId);
            throw new ToktotException(ErrorCode.INVALID_INPUT, "한 번에 최대 5개의 파일만 업로드 가능합니다.");
        }

        validateRestaurantId(restaurantId);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.debug("Upload validation failed - empty file: {}, restaurantId: {}",
                        file.getOriginalFilename(), restaurantId);
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }
        }
    }

    private void validateDeleteRequest(String imageId, Long restaurantId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            log.debug("Delete validation failed - missing imageId, restaurantId: {}", restaurantId);
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "삭제할 이미지 ID가 필요합니다.");
        }
        validateRestaurantId(restaurantId);
    }

    private void validateRestaurantId(Long restaurantId) {
        if (restaurantId == null || restaurantId <= 0) {
            log.debug("Restaurant ID validation failed: {}", restaurantId);
            throw new ToktotException(ErrorCode.INVALID_INPUT, "올바른 음식점 ID가 필요합니다.");
        }
    }
}
