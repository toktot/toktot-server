package com.toktot.domain.user.repository;

import com.toktot.domain.user.dto.response.UserInfoResponse;

import java.util.Optional;

public interface UserInfoRepositoryCustom {
    Optional<UserInfoResponse> findUserInfoWithReviewStats(Long userId);
}
