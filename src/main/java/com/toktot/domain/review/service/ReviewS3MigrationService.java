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
        log.debug("S3 migration started - userId: {}, restaurantId: {}, reviewId: {}, imageCount: {}",
                session.getUserId(), session.getRestaurantId(), reviewId, session.getImages().size());

        List<String> migratedKeys = new ArrayList<>();
        List<String> originalKeys = new ArrayList<>();

        try {
            for (ReviewImageDTO imageDTO : session.getImages()) {
                String originalKey = imageDTO.getS3Key();
                String newKey = buildReviewImageKey(session.getRestaurantId(), reviewId, imageDTO.getImageId());

                log.debug("Migrating image - imageId: {}, originalKey: {}, newKey: {}",
                        imageDTO.getImageId(), originalKey, newKey);

                copyS3Object(originalKey, newKey);
                migratedKeys.add(newKey);
                originalKeys.add(originalKey);

                String newUrl = buildImageUrl(session.getRestaurantId(), reviewId, imageDTO.getImageId());
                imageDTO.setS3Key(newKey);
                imageDTO.setImageUrl(newUrl);
            }

            deleteOriginalTempFiles(originalKeys);

            log.info("S3 migration completed - userId: {}, externalKakaoId: {}, reviewId: {}, migratedImages: {}",
                    session.getUserId(), session.getRestaurantId(), reviewId, migratedKeys.size());

        } catch (Exception e) {
            log.error("S3 migration failed - userId: {}, externalKakaoId: {}, reviewId: {}, error: {}",
                    session.getUserId(), session.getRestaurantId(), reviewId, e.getMessage(), e);

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
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.copyObject(copyRequest);

            log.debug("S3 object copied - source: {}, destination: {}", sourceKey, destinationKey);

        } catch (S3Exception e) {
            log.error("S3 copy failed - source: {}, destination: {}, errorCode: {}, statusCode: {}",
                    sourceKey, destinationKey, e.awsErrorDetails().errorCode(), e.statusCode(), e);
            throw e;

        } catch (Exception e) {
            log.error("S3 copy failed - source: {}, destination: {}, error: {}",
                    sourceKey, destinationKey, e.getMessage(), e);
            throw e;
        }
    }

    private void deleteOriginalTempFiles(List<String> tempKeys) {
        log.debug("Deleting temp files - count: {}", tempKeys.size());

        for (String tempKey : tempKeys) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(tempKey)
                        .build();

                s3Client.deleteObject(deleteRequest);

                log.debug("Temp file deleted - key: {}", tempKey);

            } catch (Exception e) {
                log.warn("Failed to delete temp file - key: {}, error: {}", tempKey, e.getMessage());
            }
        }

        log.debug("Temp files deletion completed - processed: {}", tempKeys.size());
    }

    private void rollbackMigratedFiles(List<String> migratedKeys) {
        log.warn("Starting rollback - migratedFileCount: {}", migratedKeys.size());

        for (String migratedKey : migratedKeys) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(migratedKey)
                        .build();

                s3Client.deleteObject(deleteRequest);

                log.debug("Migrated file rolled back - key: {}", migratedKey);

            } catch (Exception e) {
                log.error("Failed to rollback file - key: {}, error: {}", migratedKey, e.getMessage());
            }
        }

        log.warn("Rollback completed - processed: {}", migratedKeys.size());
    }

    private String buildReviewImageKey(Long restaurantId, Long reviewId, String imageId) {
        String extension = extractExtension(imageId);
        String filename = String.format("%s.%s", imageId, extension);
        String key = String.format("%s/%d/%d/%s", REVIEWS_PREFIX, restaurantId, reviewId, filename);

        log.trace("Built review image key - restaurantId: {}, reviewId: {}, key: {}",
                restaurantId, reviewId, key);

        return key;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String buildImageUrl(Long restaurantId, Long reviewId, String imageId) {
        return String.format("/review/%d/%d/%s", restaurantId, reviewId, imageId);
    }
}
