package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageService {

    private final ReviewS3StorageService reviewS3StorageService;
    private final ReviewSessionService reviewSessionService;

    private static final int MAX_IMAGES = 5;

    public List<ReviewImageDTO> uploadImages(List<MultipartFile> files, Long userId, Long restaurantId) {
        log.atInfo()
                .setMessage("Starting image upload process")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("requestedFileCount", files.size())
                .log();

        validateUploadRequest(files, userId, restaurantId);

        List<ReviewImageDTO> uploadedImages = new ArrayList<>();
        List<String> uploadedS3Keys = new ArrayList<>();
        int successCount = 0;

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                log.atDebug()
                        .setMessage("Processing individual file upload")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("fileIndex", i)
                        .addKeyValue("fileName", file.getOriginalFilename())
                        .addKeyValue("fileSize", file.getSize())
                        .log();

                ReviewS3StorageService.S3UploadResult uploadResult =
                        reviewS3StorageService.uploadTempImage(file, userId, restaurantId);

                log.atDebug()
                        .setMessage("S3 upload completed")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("imageId", uploadResult.getImageId())
                        .addKeyValue("s3Key", uploadResult.getS3Key())
                        .addKeyValue("fileSize", uploadResult.getFileSize())
                        .log();

                ReviewImageDTO imageDTO = ReviewImageDTO.create(
                        uploadResult.getImageId(),
                        uploadResult.getS3Key(),
                        uploadResult.getImageUrl(),
                        uploadResult.getFileSize(),
                        0
                );

                boolean added = reviewSessionService.tryAddImageToSession(userId, restaurantId, imageDTO);

                if (!added) {
                    log.atWarn()
                            .setMessage("Session addition failed - maximum images reached")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .addKeyValue("imageId", uploadResult.getImageId())
                            .addKeyValue("maxImages", MAX_IMAGES)
                            .log();

                    reviewS3StorageService.deleteTempImage(uploadResult.getS3Key());

                    log.atInfo()
                            .setMessage("Cleaned up S3 file after session addition failure")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .addKeyValue("s3Key", uploadResult.getS3Key())
                            .log();

                    throw new ToktotException(ErrorCode.OPERATION_NOT_ALLOWED,
                            "이미지는 최대 " + MAX_IMAGES + "개까지만 업로드 가능합니다.");
                }

                uploadedImages.add(imageDTO);
                uploadedS3Keys.add(uploadResult.getS3Key());
                successCount++;

                log.atDebug()
                        .setMessage("File upload and session addition completed")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("imageId", uploadResult.getImageId())
                        .addKeyValue("successCount", successCount)
                        .log();
            }

            log.atInfo()
                    .setMessage("Image upload process completed successfully")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("totalUploaded", successCount)
                    .addKeyValue("uploadedImageIds", uploadedImages.stream()
                            .map(ReviewImageDTO::getImageId)
                            .toArray())
                    .log();

            return uploadedImages;

        } catch (ToktotException e) {
            log.atWarn()
                    .setMessage("Image upload process failed - business error")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("successCount", successCount)
                    .addKeyValue("errorCode", e.getErrorCodeName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .log();
            throw e;

        } catch (Exception e) {
            log.atError()
                    .setMessage("Image upload process failed - system error")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("successCount", successCount)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            cleanupPartialUploads(uploadedS3Keys, userId, restaurantId);
            throw e;
        }
    }

    public void deleteImage(String imageId, Long userId, Long restaurantId) {
        log.atInfo()
                .setMessage("Starting image deletion process")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .log();

        ReviewSessionDTO session = reviewSessionService.getSession(userId, restaurantId)
                .orElseThrow(() -> {
                    log.atWarn()
                            .setMessage("Image deletion failed - session not found")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .addKeyValue("imageId", imageId)
                            .log();
                    return new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "세션을 찾을 수 없습니다.");
                });

        ReviewImageDTO imageToDelete = session.getImages().stream()
                .filter(img -> img.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> {
                    log.atWarn()
                            .setMessage("Image deletion failed - image not found in session")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .addKeyValue("imageId", imageId)
                            .addKeyValue("sessionImageCount", session.getImageCount())
                            .log();
                    return new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "삭제할 이미지를 찾을 수 없습니다.");
                });

        log.atDebug()
                .setMessage("Found image to delete")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .addKeyValue("s3Key", imageToDelete.getS3Key())
                .addKeyValue("fileSize", imageToDelete.getFileSize())
                .log();

        try {
            reviewS3StorageService.deleteTempImage(imageToDelete.getS3Key());

            log.atDebug()
                    .setMessage("S3 file deletion completed")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("s3Key", imageToDelete.getS3Key())
                    .log();

            reviewSessionService.removeImageFromSession(userId, restaurantId, imageId);

            log.atInfo()
                    .setMessage("Image deletion process completed successfully")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("deletedImageId", imageId)
                    .addKeyValue("remainingImages", session.getImageCount() - 1)
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("Image deletion process failed")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("s3Key", imageToDelete.getS3Key())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            throw new ToktotException(ErrorCode.SERVER_ERROR, "이미지 삭제에 실패했습니다.");
        }
    }

    public ReviewSessionDTO getCurrentSession(Long userId, Long restaurantId) {
        log.atDebug()
                .setMessage("Retrieving current session")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .log();

        ReviewSessionDTO session = reviewSessionService.getSession(userId, restaurantId)
                .orElse(ReviewSessionDTO.create(userId, restaurantId));

        log.atDebug()
                .setMessage("Current session retrieved")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("hasExistingSession", session.getImages() != null && !session.getImages().isEmpty())
                .addKeyValue("imageCount", session.getImageCount())
                .log();

        return session;
    }

    public void clearSession(Long userId, Long restaurantId) {
        log.atInfo()
                .setMessage("Starting session clearing process")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .log();

        ReviewSessionDTO session = reviewSessionService.getSession(userId, restaurantId)
                .orElse(null);

        if (session != null && session.getImages() != null) {
            int imageCount = session.getImages().size();

            log.atInfo()
                    .setMessage("Clearing S3 temporary files")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("filesToDelete", imageCount)
                    .log();

            for (ReviewImageDTO image : session.getImages()) {
                try {
                    reviewS3StorageService.deleteTempImage(image.getS3Key());

                    log.atDebug()
                            .setMessage("S3 file deleted during session clear")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .addKeyValue("imageId", image.getImageId())
                            .addKeyValue("s3Key", image.getS3Key())
                            .log();

                } catch (Exception e) {
                    log.atWarn()
                            .setMessage("Failed to delete S3 file during session clear")
                            .addKeyValue("userId", userId)
                            .addKeyValue("restaurantId", restaurantId)
                            .addKeyValue("imageId", image.getImageId())
                            .addKeyValue("s3Key", image.getS3Key())
                            .addKeyValue("error", e.getMessage())
                            .log();
                }
            }
        }

        reviewSessionService.deleteSession(userId, restaurantId);

        log.atInfo()
                .setMessage("Session clearing process completed")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("clearedImageCount", session != null ? session.getImageCount() : 0)
                .log();
    }

    private void validateUploadRequest(List<MultipartFile> files, Long userId, Long restaurantId) {
        if (files == null || files.isEmpty()) {
            log.atWarn()
                    .setMessage("Upload validation failed - no files provided")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .log();
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일이 없습니다.");
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) {
                log.atWarn()
                        .setMessage("Upload validation failed - empty file detected")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("fileIndex", i)
                        .addKeyValue("fileName", file.getOriginalFilename())
                        .log();
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }
        }

        log.atDebug()
                .setMessage("Upload validation passed")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("fileCount", files.size())
                .log();
    }

    private void cleanupPartialUploads(List<String> s3Keys, Long userId, Long restaurantId) {
        log.atWarn()
                .setMessage("Starting partial upload cleanup")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("s3KeysToCleanup", s3Keys.size())
                .log();

        for (String s3Key : s3Keys) {
            try {
                reviewS3StorageService.deleteTempImage(s3Key);

                String imageId = extractImageIdFromS3Key(s3Key);
                if (imageId != null) {
                    try {
                        reviewSessionService.removeImageFromSession(userId, restaurantId, imageId);

                        log.atDebug()
                                .setMessage("Cleaned up partial upload")
                                .addKeyValue("userId", userId)
                                .addKeyValue("restaurantId", restaurantId)
                                .addKeyValue("imageId", imageId)
                                .addKeyValue("s3Key", s3Key)
                                .log();

                    } catch (Exception e) {
                        log.atWarn()
                                .setMessage("Failed to remove from session during cleanup")
                                .addKeyValue("userId", userId)
                                .addKeyValue("restaurantId", restaurantId)
                                .addKeyValue("imageId", imageId)
                                .addKeyValue("error", e.getMessage())
                                .log();
                    }
                }
            } catch (Exception e) {
                log.atWarn()
                        .setMessage("Failed to cleanup partial upload")
                        .addKeyValue("userId", userId)
                        .addKeyValue("restaurantId", restaurantId)
                        .addKeyValue("s3Key", s3Key)
                        .addKeyValue("error", e.getMessage())
                        .log();
            }
        }

        log.atInfo()
                .setMessage("Partial upload cleanup completed")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("processedKeys", s3Keys.size())
                .log();
    }

    private String extractImageIdFromS3Key(String s3Key) {
        try {
            String[] parts = s3Key.split("/");
            if (parts.length >= 4) {
                String filename = parts[parts.length - 1];
                String imageId = filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename;

                log.atDebug()
                        .setMessage("Extracted image ID from S3 key")
                        .addKeyValue("s3Key", s3Key)
                        .addKeyValue("extractedImageId", imageId)
                        .log();

                return imageId;
            }
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("Failed to extract image ID from S3 key")
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("error", e.getMessage())
                    .log();
        }
        return null;
    }
}
