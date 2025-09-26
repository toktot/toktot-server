package com.toktot.domain.restaurant.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuImageS3StorageService {

    private final S3Client s3Client;

    @Value("${toktot.s3.bucket-name}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png"};
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png"};

    public String uploadImageAndGetImageUrl(MultipartFile file, Long restaurantId) {
        log.debug("메뉴 이미지 업로드 시작 - restaurantId: {}, fileName: {}, fileSize: {}KB",
                restaurantId, file.getOriginalFilename(), file.getSize() / 1024);

        validateFile(file);

        String imageId = UUID.randomUUID().toString();
        String s3Key = String.format("menu-submissions/%d/%s.%s",
                restaurantId, imageId, getFileExtension(file.getOriginalFilename()));

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
            log.error("S3 업로드 실패 - restaurantId: {}, fileName: {}, error: {}",
                    restaurantId, file.getOriginalFilename(), e.getMessage());
            throw new ToktotException(ErrorCode.FILE_UPLOAD_FAILED, "이미지 업로드에 실패했습니다.");
        }

        String imageUrl = String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, s3Key);
        log.debug("메뉴 이미지 업로드 완료 - restaurantId: {}, imageUrl: {}", restaurantId, imageUrl);

        return imageUrl;
    }

    public void validateMultipleFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "이미지를 업로드해 주세요.");
        }

        if (files.length > 10) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "이미지는 최대 10개까지 입력 가능합니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }
            validateFile(file);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "파일이 선택되지 않았습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ToktotException(ErrorCode.FILE_SIZE_EXCEEDED,
                    String.format("파일 크기는 %dMB를 초과할 수 없습니다: %s",
                            MAX_FILE_SIZE / (1024 * 1024), file.getOriginalFilename()));
        }

        String contentType = file.getContentType();
        boolean isValidContentType = Arrays.stream(ALLOWED_CONTENT_TYPES)
                .anyMatch(type -> type.equals(contentType));

        if (!isValidContentType) {
            throw new ToktotException(ErrorCode.INVALID_FILE_FORMAT,
                    "JPEG, PNG 파일만 업로드 가능합니다: " + file.getOriginalFilename());
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new ToktotException(ErrorCode.INVALID_INPUT, "올바른 파일명이 필요합니다.");
        }

        String extension = getFileExtension(originalFilename);
        boolean isValidExtension = Arrays.stream(ALLOWED_EXTENSIONS)
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));

        if (!isValidExtension) {
            throw new ToktotException(ErrorCode.INVALID_FILE_FORMAT,
                    "jpg, jpeg, png 확장자만 허용됩니다: " + originalFilename);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
