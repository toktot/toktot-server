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
            @RequestParam("external_kakao_id") String externalKakaoId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Image upload request - userId: {}, externalKakaoId: {}, fileCount: {}",
                user.getId(), externalKakaoId, files.length);

        validateUploadRequest(files, externalKakaoId);

        List<MultipartFile> fileList = Arrays.asList(files);
        List<ReviewImageDTO> uploadedImages = reviewImageService.uploadImages(
                fileList, user.getId(), externalKakaoId);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), externalKakaoId);
        ImageUploadResponse response = ImageUploadResponse.from(uploadedImages, session);

        log.info("Image upload completed - userId: {}, externalKakaoId: {}, uploaded: {}, total: {}",
                user.getId(), externalKakaoId, uploadedImages.size(), session.getImageCount());

        return ResponseEntity.ok(ApiResponse.success("이미지 업로드가 완료되었습니다.", response));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> deleteImage(
            @PathVariable String imageId,
            @RequestParam("external_kakao_id") String externalKakaoId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Image delete request - userId: {}, externalKakaoId: {}, imageId: {}",
                user.getId(), externalKakaoId, imageId);

        validateDeleteRequest(imageId, externalKakaoId);

        reviewImageService.deleteImage(imageId, user.getId(), externalKakaoId);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), externalKakaoId);
        SessionInfoResponse response = SessionInfoResponse.from(session);

        log.info("Image deleted - userId: {}, externalKakaoId: {}, imageId: {}, remaining: {}",
                user.getId(), externalKakaoId, imageId, session.getImageCount());

        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SessionInfoResponse>> getCurrentSession(
            @RequestParam("external_kakao_id") String externalKakaoId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Session info request - userId: {}, externalKakaoId: {}",
                user.getId(), externalKakaoId);

        validateExternalKakaoId(externalKakaoId);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), externalKakaoId);
        SessionInfoResponse response = SessionInfoResponse.from(session);

        return ResponseEntity.ok(ApiResponse.success("세션 정보를 조회했습니다.", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearSession(
            @RequestParam("external_kakao_id") String externalKakaoId,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Session clear request - userId: {}, externalKakaoId: {}",
                user.getId(), externalKakaoId);

        validateExternalKakaoId(externalKakaoId);

        ReviewSessionDTO sessionBeforeClear = reviewImageService.getCurrentSession(user.getId(), externalKakaoId);
        int imageCountBeforeClear = sessionBeforeClear.getImageCount();

        reviewImageService.clearSession(user.getId(), externalKakaoId);

        log.info("Session cleared - userId: {}, externalKakaoId: {}, clearedCount: {}",
                user.getId(), externalKakaoId, imageCountBeforeClear);

        return ResponseEntity.ok(ApiResponse.success("세션이 초기화되었습니다.", "cleared"));
    }

    private void validateUploadRequest(MultipartFile[] files, String externalKakaoId) {
        if (files == null || files.length == 0) {
            log.debug("Upload validation failed - no files, externalKakaoId: {}", externalKakaoId);
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일을 선택해주세요.");
        }

        if (files.length > 5) {
            log.debug("Upload validation failed - too many files: {}, externalKakaoId: {}",
                    files.length, externalKakaoId);
            throw new ToktotException(ErrorCode.INVALID_INPUT, "한 번에 최대 5개의 파일만 업로드 가능합니다.");
        }

        validateExternalKakaoId(externalKakaoId);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.debug("Upload validation failed - empty file: {}, externalKakaoId: {}",
                        file.getOriginalFilename(), externalKakaoId);
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }
        }
    }

    private void validateDeleteRequest(String imageId, String externalKakaoId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            log.debug("Delete validation failed - missing imageId, externalKakaoId: {}", externalKakaoId);
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "삭제할 이미지 ID가 필요합니다.");
        }
        validateExternalKakaoId(externalKakaoId);
    }

    private void validateExternalKakaoId(String externalKakaoId) {
        if (externalKakaoId == null) {
            log.debug("ExternalKakaoId is null");
            throw new ToktotException(ErrorCode.INVALID_INPUT, "올바른 음식점 ID가 필요합니다.");
        }
    }
}
