package com.toktot.domain.block;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerUser_IdAndBlockedUser_Id(Long blockerUserId, Long blockedUserId);
}
