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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewS3StorageService {

    private final S3Client s3Client;

    @Value("${toktot.s3.bucket-name}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png"};

    public S3UploadResult uploadTempImage(MultipartFile file, Long userId, Long restaurantId) {
        validateFile(file);

        String imageId = UUID.randomUUID().toString();
        String s3Key = String.format("temp/%d/%d/%s.%s",
                userId, restaurantId, imageId, getFileExtension(file.getOriginalFilename()));

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new ToktotException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String imageUrl = String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, s3Key);

        return S3UploadResult.builder()
                .imageId(imageId)
                .s3Key(s3Key)
                .imageUrl(imageUrl)
                .fileSize(file.getSize())
                .build();
    }

    public void deleteTempImage(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "파일이 선택되지 않았습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ToktotException(ErrorCode.FILE_SIZE_EXCEEDED,
                    String.format("파일 크기는 %dMB를 초과할 수 없습니다.", MAX_FILE_SIZE / (1024 * 1024)));
        }

        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                isValidType = true;
                break;
            }
        }

        if (!isValidType) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "JPEG, PNG 파일만 업로드 가능합니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "올바른 파일명이 필요합니다.");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    @lombok.Builder
    @lombok.Getter
    public static class S3UploadResult {
        private final String imageId;
        private final String s3Key;
        private final String imageUrl;
        private final Long fileSize;
    }
}
