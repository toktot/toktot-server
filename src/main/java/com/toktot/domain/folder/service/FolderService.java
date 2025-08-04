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
import com.toktot.web.dto.folder.response.FolderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public FolderResponse createFolder(User user, String folderName) {
        Folder folder = Folder.create(user, folderName);
        folderRepository.save(folder);

        return FolderResponse.from(folder);
    }

    public List<FolderResponse> readFolders(User user) {
        return folderRepository.findFoldersWithReviewCountByUserId(user.getId());
    }

    @Transactional
    public void createFolderReviews(User user, List<Long> folderIds, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ToktotException(ErrorCode.REVIEW_NOT_FOUND));

        for (Long folderId : folderIds) {
            createFolderReview(user, folderId, review);
        }
    }

    private void createFolderReview(User user, Long folderId, Review review) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ToktotException(ErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new ToktotException(ErrorCode.ACCESS_DENIED, "권한이 없는 폴더입니다.");
        }

        folderReviewRepository.save(FolderReview.create(folder, review));
    }

}
