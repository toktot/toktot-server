package com.toktot.domain.review.controller;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import com.toktot.domain.review.service.ReviewImageService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.review.dto.response.create.ImageUploadResponse;
import com.toktot.domain.review.dto.response.create.SessionInfoResponse;
import jakarta.validation.constraints.Positive;
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
            @RequestParam("id") @Positive Long id,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Image upload request - user.id: {}, restaurant.id: {}, files.length: {}",
                user.getId(), id, files.length);

        validateUploadRequest(files, id);

        List<MultipartFile> fileList = Arrays.asList(files);
        List<ReviewImageDTO> uploadedImages = reviewImageService.uploadImages(fileList, user.getId(), id);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), id);
        ImageUploadResponse response = ImageUploadResponse.from(uploadedImages, session);

        return ResponseEntity.ok(ApiResponse.success("이미지 업로드가 완료되었습니다.", response));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> deleteImage(
            @PathVariable String imageId,
            @RequestParam("id") @Positive Long id,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Image delete request - user.id: {}, restaurant.id: {}, imageId: {}",
                user.getId(), id, imageId);

        reviewImageService.deleteImage(imageId, user.getId(), id);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), id);
        SessionInfoResponse response = SessionInfoResponse.from(session);

        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SessionInfoResponse>> getCurrentSession(
            @RequestParam("id") @Positive Long id,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Session info request - user.id: {}, restaurant.id: {}",
                user.getId(), id);

        ReviewSessionDTO session = reviewImageService.getCurrentSession(user.getId(), id);
        SessionInfoResponse response = SessionInfoResponse.from(session);

        return ResponseEntity.ok(ApiResponse.success("세션 정보를 조회했습니다.", response));
    }

    @DeleteMapping("/clear/{id}")
    public ResponseEntity<ApiResponse<String>> clearSession(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal User user
    ) {
        log.debug("Session clear request - user.id: {}, restaurant.id: {}",
                user.getId(), id);

        reviewImageService.clearSession(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("세션이 초기화되었습니다.", "cleared"));
    }

    private void validateUploadRequest(MultipartFile[] files, Long id) {
        if (files == null || files.length == 0) {
            log.debug("Upload validation failed - no files, restaurant.id: {}", id);
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일을 선택해주세요.");
        }

        if (files.length > 5) {
            log.debug("Upload validation failed - too many files: {}, restaurant.id: {}",
                    files.length, id);
            throw new ToktotException(ErrorCode.INVALID_INPUT, "한 번에 최대 5개의 파일만 업로드 가능합니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                log.debug("Upload validation failed - empty file: {}, restaurant.id: {}",
                        file.getOriginalFilename(), id);
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                log.debug("Upload validation failed - file too large: {}, size: {}, restaurant.id: {}",
                        file.getOriginalFilename(), file.getSize(), id);
                throw new ToktotException(ErrorCode.FILE_SIZE_EXCEEDED,
                        "파일 크기는 5MB를 초과할 수 없습니다: " + file.getOriginalFilename());
            }
        }
    }

}
