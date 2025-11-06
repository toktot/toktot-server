package com.toktot.domain.user.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.folder.service.FolderService;
import com.toktot.domain.review.service.ReviewSessionService;
import com.toktot.domain.user.User;
import com.toktot.domain.user.dto.response.UserInfoResponse;
import com.toktot.domain.user.repository.UserInfoRepositoryImpl;
import com.toktot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ReviewSessionService reviewSessionService;
    private final FolderService folderService;
    private final UserInfoRepositoryImpl userInfoRepository;

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        deleteUserRelatedData(userId);
        user.softDelete();
    }

    public UserInfoResponse getUserInfo(Long userId) {
        return userInfoRepository.findUserInfoWithReviewStats(userId)
                .orElseThrow(() -> new ToktotException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ToktotException(ErrorCode.USER_NOT_FOUND));
    }

    private void deleteUserRelatedData(Long userId) {
        reviewSessionService.deleteAllUserSessions(userId);
        folderService.deleteByUserId(userId);
    }
}
