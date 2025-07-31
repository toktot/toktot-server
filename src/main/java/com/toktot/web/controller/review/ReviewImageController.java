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
@RequestMapping("/api/v1/reviews/images")
@RequiredArgsConstructor
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        long totalFileSize = Arrays.stream(files).mapToLong(MultipartFile::getSize).sum();

        log.atInfo()
                .setMessage("Image upload request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("fileCount", files.length)
                .addKeyValue("totalFileSize", totalFileSize)
                .addKeyValue("fileNames", Arrays.stream(files)
                        .map(MultipartFile::getOriginalFilename)
                        .toArray())
                .log();

        try {
            validateUploadRequest(files, restaurantId);

            List<MultipartFile> fileList = Arrays.asList(files);
            List<ReviewImageDTO> uploadedImages = reviewImageService.uploadImages(
                    fileList, user.getId(), restaurantId);

            ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), restaurantId);
            ImageUploadResponse response = ImageUploadResponse.from(uploadedImages, session);

            log.atInfo()
                    .setMessage("Image upload request completed successfully")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("uploadedCount", uploadedImages.size())
                    .addKeyValue("totalSessionImages", session.getImageCount())
                    .addKeyValue("remainingSlots", 5 - session.getImageCount())
                    .addKeyValue("uploadedImageIds", uploadedImages.stream()
                            .map(ReviewImageDTO::getImageId)
                            .toArray())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success("이미지 업로드가 완료되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Image upload request failed - business error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("fileCount", files.length)
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .addKeyValue("isValidationError", e.isValidationError())
                    .addKeyValue("isSystemError", e.isSystemError())
                    .log();

            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Image upload request failed - system error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("fileCount", files.length)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("exceptionType", e.getClass().getSimpleName())
                    .setCause(e)
                    .log();

            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> deleteImage(
            @PathVariable String imageId,
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.atInfo()
                .setMessage("Image delete request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .log();

        try {
            validateDeleteRequest(imageId, restaurantId);

            reviewImageService.deleteImage(imageId, user.getId(), restaurantId);

            ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), restaurantId);
            SessionInfoResponse response = SessionInfoResponse.from(session);

            log.atInfo()
                    .setMessage("Image delete request completed successfully")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("deletedImageId", imageId)
                    .addKeyValue("remainingImages", session.getImageCount())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success("이미지가 삭제되었습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Image delete request failed - business error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .log();

            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Image delete request failed - system error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SessionInfoResponse>> getCurrentSession(
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.atDebug()
                .setMessage("Session info request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", restaurantId)
                .log();

        try {
            validateRestaurantId(restaurantId);

            ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), restaurantId);
            SessionInfoResponse response = SessionInfoResponse.from(session);

            log.atDebug()
                    .setMessage("Session info request completed")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("hasSession", response.hasSession())
                    .addKeyValue("imageCount", response.totalImageCount())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success("세션 정보를 조회했습니다.", response)
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Session info request failed - business error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .log();

            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Session info request failed - system error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearSession(
            @RequestParam("restaurant_id") Long restaurantId,
            @AuthenticationPrincipal User user
    ) {
        log.atInfo()
                .setMessage("Session clear request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("restaurantId", restaurantId)
                .log();

        try {
            validateRestaurantId(restaurantId);

            ReviewSessionDTO sessionBeforeClear = reviewImageService.getCurrentSession(user.getId(), restaurantId);
            int imageCountBeforeClear = sessionBeforeClear.getImageCount();

            reviewImageService.clearSession(user.getId(), restaurantId);

            log.atInfo()
                    .setMessage("Session clear request completed successfully")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("clearedImageCount", imageCountBeforeClear)
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success("세션이 초기화되었습니다.", "cleared")
            );

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Session clear request failed - business error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .log();

            return ResponseEntity.ok(ApiResponse.error(e.getErrorCode(), e.getMessage()));

        } catch (Exception e) {
            log.atError()
                    .setMessage("Session clear request failed - system error")
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            return ResponseEntity.ok(ApiResponse.error(ErrorCode.SERVER_ERROR));
        }
    }

    private void validateUploadRequest(MultipartFile[] files, Long restaurantId) {
        if (files == null || files.length == 0) {
            log.atWarn()
                    .setMessage("Upload validation failed - no files")
                    .addKeyValue("restaurantId", restaurantId)
                    .log();
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일을 선택해주세요.");
        }

        if (files.length > 5) {
            log.atWarn()
                    .setMessage("Upload validation failed - too many files")
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("fileCount", files.length)
                    .addKeyValue("maxAllowed", 5)
                    .log();
            throw new ToktotException(ErrorCode.INVALID_INPUT, "한 번에 최대 5개의 파일만 업로드 가능합니다.");
        }

        validateRestaurantId(restaurantId);

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file.isEmpty()) {
                log.atWarn()
                        .setMessage("Upload validation failed - empty file detected")
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("fileIndex", i)
                        .addKeyValue("fileName", file.getOriginalFilename())
                        .log();
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }
        }

        log.atDebug()
                .setMessage("Upload validation passed")
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("fileCount", files.length)
                .log();
    }

    private void validateDeleteRequest(String imageId, Long restaurantId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            log.atWarn()
                    .setMessage("Delete validation failed - missing image ID")
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .log();
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "삭제할 이미지 ID가 필요합니다.");
        }

        validateRestaurantId(restaurantId);

        log.atDebug()
                .setMessage("Delete validation passed")
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .log();
    }

    private void validateRestaurantId(Long restaurantId) {
        if (restaurantId == null || restaurantId <= 0) {
            log.atWarn()
                    .setMessage("Restaurant ID validation failed")
                    .addKeyValue("restaurantId", restaurantId)
                    .log();
            throw new ToktotException(ErrorCode.INVALID_INPUT, "올바른 음식점 ID가 필요합니다.");
        }
    }
}
