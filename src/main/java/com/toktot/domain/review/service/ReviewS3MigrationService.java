package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewS3MigrationService {

    private final S3Client s3Client;

    @Value("${toktot.s3.bucket-name}")
    private String bucketName;

    @Value("${toktot.s3.region}")
    private String region;

    private static final String REVIEWS_PREFIX = "reviews";

    public void migrateSessionImages(ReviewSessionDTO session, Long reviewId) {
        log.atInfo()
                .setMessage("Starting S3 image migration from temp to reviews")
                .addKeyValue("userId", session.getUserId())
                .addKeyValue("restaurantId", session.getRestaurantId())
                .addKeyValue("reviewId", reviewId)
                .addKeyValue("imageCount", session.getImages().size())
                .log();

        List<String> migratedKeys = new ArrayList<>();
        List<String> originalKeys = new ArrayList<>();

        try {
            for (ReviewImageDTO imageDTO : session.getImages()) {
                String originalKey = imageDTO.getS3Key();
                String newKey = buildReviewImageKey(session.getRestaurantId(), reviewId,
                        imageDTO.getOrder(), imageDTO.getImageId());

                log.atDebug()
                        .setMessage("Migrating individual image")
                        .addKeyValue("imageId", imageDTO.getImageId())
                        .addKeyValue("originalKey", originalKey)
                        .addKeyValue("newKey", newKey)
                        .addKeyValue("order", imageDTO.getOrder())
                        .log();

                copyS3Object(originalKey, newKey);
                migratedKeys.add(newKey);
                originalKeys.add(originalKey);

                String newUrl = buildImageUrl(newKey);
                imageDTO.setS3Key(newKey);
                imageDTO.setImageUrl(newUrl);

                log.atDebug()
                        .setMessage("Image migration completed")
                        .addKeyValue("imageId", imageDTO.getImageId())
                        .addKeyValue("newKey", newKey)
                        .addKeyValue("newUrl", newUrl)
                        .log();
            }

            deleteOriginalTempFiles(originalKeys);

            log.atInfo()
                    .setMessage("S3 image migration completed successfully")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .addKeyValue("reviewId", reviewId)
                    .addKeyValue("migratedImages", migratedKeys.size())
                    .addKeyValue("deletedTempFiles", originalKeys.size())
                    .log();

        } catch (Exception e) {
            log.atError()
                    .setMessage("S3 image migration failed - starting rollback")
                    .addKeyValue("userId", session.getUserId())
                    .addKeyValue("restaurantId", session.getRestaurantId())
                    .addKeyValue("reviewId", reviewId)
                    .addKeyValue("migratedCount", migratedKeys.size())
                    .addKeyValue("totalImages", session.getImages().size())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();

            rollbackMigratedFiles(migratedKeys);

            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "이미지 저장에 실패했습니다. 다시 시도해주세요.");
        }
    }

    private void copyS3Object(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyRequest);

            log.atDebug()
                    .setMessage("S3 object copied successfully")
                    .addKeyValue("sourceKey", sourceKey)
                    .addKeyValue("destinationKey", destinationKey)
                    .addKeyValue("bucket", bucketName)
                    .log();

        } catch (S3Exception e) {
            log.atError()
                    .setMessage("S3 object copy failed")
                    .addKeyValue("sourceKey", sourceKey)
                    .addKeyValue("destinationKey", destinationKey)
                    .addKeyValue("bucket", bucketName)
                    .addKeyValue("awsErrorCode", e.awsErrorDetails().errorCode())
                    .addKeyValue("awsErrorMessage", e.awsErrorDetails().errorMessage())
                    .addKeyValue("statusCode", e.statusCode())
                    .setCause(e)
                    .log();
            throw e;

        } catch (Exception e) {
            log.atError()
                    .setMessage("S3 object copy failed - unexpected error")
                    .addKeyValue("sourceKey", sourceKey)
                    .addKeyValue("destinationKey", destinationKey)
                    .addKeyValue("bucket", bucketName)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw e;
        }
    }

    private void deleteOriginalTempFiles(List<String> tempKeys) {
        log.atInfo()
                .setMessage("Deleting original temp files")
                .addKeyValue("tempFileCount", tempKeys.size())
                .log();

        for (String tempKey : tempKeys) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(tempKey)
                        .build();

                s3Client.deleteObject(deleteRequest);

                log.atDebug()
                        .setMessage("Temp file deleted successfully")
                        .addKeyValue("tempKey", tempKey)
                        .addKeyValue("bucket", bucketName)
                        .log();

            } catch (Exception e) {
                log.atWarn()
                        .setMessage("Failed to delete temp file - will be cleaned up by batch job")
                        .addKeyValue("tempKey", tempKey)
                        .addKeyValue("bucket", bucketName)
                        .addKeyValue("error", e.getMessage())
                        .log();
            }
        }

        log.atInfo()
                .setMessage("Original temp files deletion completed")
                .addKeyValue("processedFiles", tempKeys.size())
                .log();
    }

    private void rollbackMigratedFiles(List<String> migratedKeys) {
        log.atWarn()
                .setMessage("Starting rollback - deleting migrated files")
                .addKeyValue("migratedFileCount", migratedKeys.size())
                .log();

        for (String migratedKey : migratedKeys) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(migratedKey)
                        .build();

                s3Client.deleteObject(deleteRequest);

                log.atDebug()
                        .setMessage("Migrated file rolled back successfully")
                        .addKeyValue("migratedKey", migratedKey)
                        .addKeyValue("bucket", bucketName)
                        .log();

            } catch (Exception e) {
                log.atError()
                        .setMessage("Failed to rollback migrated file")
                        .addKeyValue("migratedKey", migratedKey)
                        .addKeyValue("bucket", bucketName)
                        .addKeyValue("error", e.getMessage())
                        .log();
            }
        }

        log.atWarn()
                .setMessage("Rollback completed")
                .addKeyValue("processedFiles", migratedKeys.size())
                .log();
    }

    private String buildReviewImageKey(Long restaurantId, Long reviewId, int order, String imageId) {
        String extension = extractExtension(imageId);
        String filename = String.format("%d_%s.%s", order, imageId, extension);
        String key = String.format("%s/%d/%d/%s", REVIEWS_PREFIX, restaurantId, reviewId, filename);

        log.atTrace()
                .setMessage("Built review image key")
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("reviewId", reviewId)
                .addKeyValue("order", order)
                .addKeyValue("imageId", imageId)
                .addKeyValue("key", key)
                .log();

        return key;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String buildImageUrl(String s3Key) {
        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);

        log.atTrace()
                .setMessage("Built image URL")
                .addKeyValue("s3Key", s3Key)
                .addKeyValue("bucket", bucketName)
                .addKeyValue("region", region)
                .addKeyValue("imageUrl", imageUrl)
                .log();

        return imageUrl;
    }
}
