package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewS3StorageService {

    private final S3Client s3Client;

    @Value("${toktot.s3.bucket-name}")
    private String bucketName;

    @Value("${toktot.s3.region}")
    private String region;

    private static final String TEMP_PREFIX = "temp";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public S3UploadResult uploadTempImage(MultipartFile file, Long userId, Long restaurantId) {
        log.atInfo()
                .setMessage("Starting S3 temporary image upload")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("fileName", file.getOriginalFilename())
                .addKeyValue("fileSize", file.getSize())
                .addKeyValue("contentType", file.getContentType())
                .log();

        validateFile(file);

        String imageId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String s3Key = buildTempS3Key(userId, restaurantId, imageId, fileExtension);

        log.atDebug()
                .setMessage("Generated S3 upload parameters")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .addKeyValue("s3Key", s3Key)
                .addKeyValue("fileExtension", fileExtension)
                .log();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String imageUrl = buildImageUrl(s3Key);
            S3UploadResult result = new S3UploadResult(imageId, s3Key, imageUrl, file.getSize());

            log.atInfo()
                    .setMessage("S3 temporary image upload completed successfully")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("imageUrl", imageUrl)
                    .addKeyValue("fileSize", file.getSize())
                    .log();

            return result;

        } catch (IOException e) {
            log.atError()
                    .setMessage("S3 upload failed - IO error")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("fileName", file.getOriginalFilename())
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw new ToktotException(ErrorCode.FILE_UPLOAD_FAILED, "파일 업로드에 실패했습니다.");

        } catch (S3Exception e) {
            log.atError()
                    .setMessage("S3 upload failed - S3 service error")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("bucket", bucketName)
                    .addKeyValue("awsErrorCode", e.awsErrorDetails().errorCode())
                    .addKeyValue("awsErrorMessage", e.awsErrorDetails().errorMessage())
                    .addKeyValue("statusCode", e.statusCode())
                    .setCause(e)
                    .log();
            throw new ToktotException(ErrorCode.EXTERNAL_SERVICE_ERROR, "이미지 저장에 실패했습니다.");

        } catch (Exception e) {
            log.atError()
                    .setMessage("S3 upload failed - unexpected error")
                    .addKeyValue("userId", userId)
                    .addKeyValue("restaurantId", restaurantId)
                    .addKeyValue("imageId", imageId)
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("error", e.getMessage())
                    .setCause(e)
                    .log();
            throw new ToktotException(ErrorCode.SERVER_ERROR, "서버 오류가 발생했습니다.");
        }
    }

    public void deleteTempImage(String s3Key) {
        log.atInfo()
                .setMessage("Starting S3 temporary image deletion")
                .addKeyValue("s3Key", s3Key)
                .addKeyValue("bucket", bucketName)
                .log();

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.atInfo()
                    .setMessage("S3 temporary image deletion completed successfully")
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("bucket", bucketName)
                    .log();

        } catch (S3Exception e) {
            log.atWarn()
                    .setMessage("S3 image deletion failed - S3 service error")
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("bucket", bucketName)
                    .addKeyValue("awsErrorCode", e.awsErrorDetails().errorCode())
                    .addKeyValue("awsErrorMessage", e.awsErrorDetails().errorMessage())
                    .addKeyValue("statusCode", e.statusCode())
                    .log();

        } catch (Exception e) {
            log.atWarn()
                    .setMessage("S3 image deletion failed - unexpected error")
                    .addKeyValue("s3Key", s3Key)
                    .addKeyValue("bucket", bucketName)
                    .addKeyValue("error", e.getMessage())
                    .log();
        }
    }

    private void validateFile(MultipartFile file) {
        log.atDebug()
                .setMessage("Starting file validation")
                .addKeyValue("fileName", file.getOriginalFilename())
                .addKeyValue("fileSize", file.getSize())
                .addKeyValue("contentType", file.getContentType())
                .addKeyValue("maxFileSize", MAX_FILE_SIZE)
                .log();

        if (file == null || file.isEmpty()) {
            log.atWarn()
                    .setMessage("File validation failed - file is null or empty")
                    .addKeyValue("fileName", file != null ? file.getOriginalFilename() : "null")
                    .log();
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "파일이 선택되지 않았습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.atWarn()
                    .setMessage("File validation failed - file size exceeds limit")
                    .addKeyValue("fileName", file.getOriginalFilename())
                    .addKeyValue("fileSize", file.getSize())
                    .addKeyValue("maxFileSize", MAX_FILE_SIZE)
                    .addKeyValue("fileSizeMB", file.getSize() / 1024.0 / 1024.0)
                    .addKeyValue("maxFileSizeMB", MAX_FILE_SIZE / 1024.0 / 1024.0)
                    .log();
            throw new ToktotException(ErrorCode.FILE_SIZE_EXCEEDED, "파일 크기는 5MB 이하여야 합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            log.atWarn()
                    .setMessage("File validation failed - unsupported content type")
                    .addKeyValue("fileName", file.getOriginalFilename())
                    .addKeyValue("contentType", contentType)
                    .addKeyValue("allowedTypes", new String[]{"image/jpeg", "image/png"})
                    .log();
            throw new ToktotException(ErrorCode.INVALID_FILE_FORMAT, "JPEG, PNG 파일만 업로드 가능합니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.contains("..") || originalFilename.contains("/"))) {
            log.atWarn()
                    .setMessage("File validation failed - unsafe filename detected")
                    .addKeyValue("fileName", originalFilename)
                    .log();
            throw new ToktotException(ErrorCode.INVALID_INPUT, "안전하지 않은 파일명입니다.");
        }

        log.atDebug()
                .setMessage("File validation passed")
                .addKeyValue("fileName", file.getOriginalFilename())
                .addKeyValue("fileSize", file.getSize())
                .addKeyValue("contentType", file.getContentType())
                .log();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            log.atDebug()
                    .setMessage("Using default file extension")
                    .addKeyValue("originalFilename", filename)
                    .addKeyValue("defaultExtension", "jpg")
                    .log();
            return "jpg";
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        log.atDebug()
                .setMessage("Extracted file extension")
                .addKeyValue("originalFilename", filename)
                .addKeyValue("extractedExtension", extension)
                .log();

        return extension;
    }

    private String buildTempS3Key(Long userId, Long restaurantId, String imageId, String extension) {
        String s3Key = String.format("%s/%d/%d/%s.%s", TEMP_PREFIX, userId, restaurantId, imageId, extension);

        log.atDebug()
                .setMessage("Built temporary S3 key")
                .addKeyValue("userId", userId)
                .addKeyValue("restaurantId", restaurantId)
                .addKeyValue("imageId", imageId)
                .addKeyValue("extension", extension)
                .addKeyValue("s3Key", s3Key)
                .log();

        return s3Key;
    }

    private String buildImageUrl(String s3Key) {
        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);

        log.atDebug()
                .setMessage("Built image URL")
                .addKeyValue("s3Key", s3Key)
                .addKeyValue("bucket", bucketName)
                .addKeyValue("region", region)
                .addKeyValue("imageUrl", imageUrl)
                .log();

        return imageUrl;
    }

    public static class S3UploadResult {
        private final String imageId;
        private final String s3Key;
        private final String imageUrl;
        private final long fileSize;

        public S3UploadResult(String imageId, String s3Key, String imageUrl, long fileSize) {
            this.imageId = imageId;
            this.s3Key = s3Key;
            this.imageUrl = imageUrl;
            this.fileSize = fileSize;
        }

        public String getImageId() { return imageId; }
        public String getS3Key() { return s3Key; }
        public String getImageUrl() { return imageUrl; }
        public long getFileSize() { return fileSize; }
    }
}
