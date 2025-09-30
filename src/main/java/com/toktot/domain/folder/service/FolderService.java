package com.toktot.domain.folder.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.folder.Folder;
import com.toktot.domain.folder.FolderReview;
import com.toktot.domain.folder.repository.FolderRepository;
import com.toktot.domain.folder.repository.FolderReviewRepository;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.user.User;
import com.toktot.domain.folder.dto.response.FolderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FolderService {

    private final ReviewRepository reviewRepository;
    private final FolderRepository folderRepository;
    private final FolderReviewRepository folderReviewRepository;

    @Lazy
    private final FolderDefaultService folderDefaultService;

    @Transactional
    public FolderResponse createFolder(User user, String folderName) {
        folderDefaultService.ensureDefaultFolderExists(user);

        Folder folder = Folder.createNewFolder(user, folderName);
        folderRepository.save(folder);

        return FolderResponse.fromNewFolder(folder);
    }

    public List<FolderResponse> readFolders(User user) {
        folderDefaultService.ensureDefaultFolderExists(user);
        return folderRepository.findFoldersWithReviewCountByUserId(user.getId());
    }

    public List<FolderResponse> readUserFolders(Long userId) {
        return folderRepository.findFoldersWithReviewCountByUserId(userId);
    }

    @Transactional
    public FolderResponse updateFolderName(User user, Long folderId, String newFolderName) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ToktotException(ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new ToktotException(ErrorCode.ACCESS_DENIED, "권한이 없는 폴더입니다.");
        }

        folder.updateFolderName(newFolderName);
        log.info("폴더명 변경 완료 - folderId: {}, userId: {}, newName: {}", folderId, user.getId(), newFolderName);

        return FolderResponse.fromNewFolder(folder);
    }

    @Transactional
    public void deleteFolder(User user, Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ToktotException(ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new ToktotException(ErrorCode.ACCESS_DENIED, "권한이 없는 폴더입니다.");
        }

        if (folder.getIsDefault()) {
            throw new ToktotException(ErrorCode.DEFAULT_FOLDER_CANNOT_DELETE);
        }

        folderRepository.delete(folder);
        log.info("폴더 삭제 완료 - folderId: {}, userId: {}", folderId, user.getId());
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        List<Folder> folders = folderRepository.findAllByUserId(userId);
        folderRepository.deleteAll(folders);
    }

    @Transactional
    public void createFolderReviews(User user, List<Long> folderIds, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ToktotException(ErrorCode.REVIEW_NOT_FOUND));

        for (Long folderId : folderIds) {
            createFolderReview(user, folderId, review);
        }
    }

    @Transactional
    public void deleteFolderReview(User user, Long folderReviewId) {
        FolderReview folderReview = folderReviewRepository.findById(folderReviewId)
                .orElseThrow(() -> new ToktotException(ErrorCode.FOLDER_REVIEW_NOT_FOUND));

        if (!folderReview.getFolder().getUser().getId().equals(user.getId())) {
            throw new ToktotException(ErrorCode.ACCESS_DENIED, "권한이 없는 폴더입니다.");
        }

        folderReviewRepository.delete(folderReview);
        log.info("폴더에 저장된 리뷰 삭제 완료 - folderReviewId: {}, userId: {}", folderReviewId, user.getId());
    }

    private void createFolderReview(User user, Long folderId, Review review) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ToktotException(ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new ToktotException(ErrorCode.ACCESS_DENIED, "권한이 없는 폴더입니다.");
        }

        if (folderReviewRepository.existsByFolderIdAndReviewId(folderId, review.getId())) {
            log.debug("이미 저장된 리뷰 - folderId: {}, reviewId: {}", folderId, review.getId());
            return;
        }

        folderReviewRepository.save(FolderReview.create(folder, review));
        log.debug("리뷰 저장 완료 - folderId: {}, reviewId: {}", folderId, review.getId());
    }

}
