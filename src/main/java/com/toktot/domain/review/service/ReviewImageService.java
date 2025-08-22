package com.toktot.domain.review.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.ReviewImage;
import com.toktot.domain.review.Tooltip;
import com.toktot.domain.review.dto.ReviewImageDTO;
import com.toktot.domain.review.dto.ReviewSessionDTO;
import com.toktot.web.dto.review.request.ReviewImageRequest;
import com.toktot.web.dto.review.request.TooltipRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageService {

    private final ReviewS3StorageService reviewS3StorageService;
    private final ReviewSessionService reviewSessionService;

    private static final int MAX_IMAGES = 5;

    public List<ReviewImageDTO> uploadImages(List<MultipartFile> files, Long userId, String externalKakaoId) {
        validateUploadRequest(files);

        List<ReviewImageDTO> uploadedImages = new ArrayList<>();
        List<String> uploadedS3Keys = new ArrayList<>();

        for (MultipartFile file : files) {
            ReviewS3StorageService.S3UploadResult uploadResult =
                    reviewS3StorageService.uploadTempImage(file, userId, externalKakaoId);

            ReviewImageDTO imageDTO = ReviewImageDTO.create(
                    uploadResult.getImageId(),
                    uploadResult.getS3Key(),
                    uploadResult.getImageUrl(),
                    uploadResult.getFileSize(),
                    0
            );

            boolean added = reviewSessionService.tryAddImageToSession(userId, externalKakaoId, imageDTO);
            if (!added) {
                reviewS3StorageService.deleteTempImage(uploadResult.getS3Key());
                throw new ToktotException(ErrorCode.OPERATION_NOT_ALLOWED, "이미지는 최대 " + MAX_IMAGES + "개까지만 업로드 가능합니다.");
            }

            uploadedImages.add(imageDTO);
            uploadedS3Keys.add(uploadResult.getS3Key());
        }

        log.info("Image upload completed - userId: {}, externalKakaoId: {}, count: {}",
                userId, externalKakaoId, uploadedImages.size());
        return uploadedImages;
    }

    public void deleteImage(String imageId, Long userId, String externalKakaoId) {
        ReviewSessionDTO session = reviewSessionService.getSession(userId, externalKakaoId)
                .orElseThrow(() -> new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "세션을 찾을 수 없습니다."));

        ReviewImageDTO imageToDelete = session.getImages().stream()
                .filter(img -> img.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ToktotException(ErrorCode.RESOURCE_NOT_FOUND, "삭제할 이미지를 찾을 수 없습니다."));

        reviewS3StorageService.deleteTempImage(imageToDelete.getS3Key());
        reviewSessionService.removeImageFromSession(userId, externalKakaoId, imageId);

    }

    public ReviewSessionDTO getCurrentSession(Long userId, String externalKakaoId) {
        return reviewSessionService.getSession(userId, externalKakaoId)
                .orElse(ReviewSessionDTO.create(userId, externalKakaoId));
    }

    public void clearSession(Long userId, String externalKakaoId) {
        ReviewSessionDTO session = reviewSessionService.getSession(userId, externalKakaoId)
                .orElse(null);

        if (session != null && session.getImages() != null) {
            for (ReviewImageDTO image : session.getImages()) {
                try {
                    reviewS3StorageService.deleteTempImage(image.getS3Key());
                } catch (Exception e) {
                    log.warn("Failed to delete S3 file during session clear - key: {}", image.getS3Key());
                }
            }
        }

        reviewSessionService.deleteSession(userId, externalKakaoId);
        log.info("Session cleared - userId: {}, externalKakaoId: {}", userId, externalKakaoId);
    }

    private void validateUploadRequest(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ToktotException(ErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일이 없습니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new ToktotException(ErrorCode.INVALID_INPUT, "빈 파일은 업로드할 수 없습니다.");
            }
        }
    }

    @Transactional
    public void saveImagesInReview(Review review, List<ReviewImageRequest> imageRequests, ReviewSessionDTO session) {
        Map<String, ReviewImageDTO> sessionImageMap = session.getImages().stream()
                .collect(Collectors.toMap(ReviewImageDTO::getImageId, Function.identity()));

        for (ReviewImageRequest imageRequest : imageRequests) {
            ReviewImageDTO reviewImageDTO = sessionImageMap.get(imageRequest.imageId());
            ReviewImage reviewImage = createReviewImageFromRequest(reviewImageDTO, imageRequest);

            if (imageRequest.tooltips() != null) {
                for (TooltipRequest tooltipRequest : imageRequest.tooltips()) {
                    Tooltip tooltip = createTooltipFromRequest(tooltipRequest);
                    reviewImage.addTooltip(tooltip);
                }
            }

            review.addImage(reviewImage);
        }
    }

    private ReviewImage createReviewImageFromRequest(ReviewImageDTO sessionImage, ReviewImageRequest reviewImageRequest) {
        return ReviewImage.create(
                sessionImage.getImageId(),
                sessionImage.getS3Key(),
                sessionImage.getImageUrl(),
                sessionImage.getFileSize(),
                reviewImageRequest.order(),
                reviewImageRequest.isMain()
        );
    }

    private Tooltip createTooltipFromRequest(TooltipRequest request) {
        return Tooltip.create(
                request.xPosition(),
                request.yPosition(),
                request.menuName(),
                request.totalPrice(),
                request.servingSize(),
                request.rating(),
                request.detailedReview()
        );
    }

}
