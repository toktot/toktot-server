package com.toktot.domain.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerUser_IdAndBlockedUser_Id(Long blockerUserId, Long blockedUserId);

    @Query("""
    SELECT ub.blockedUser.id
    FROM UserBlock ub
    WHERE ub.blockerUser.id = :blockerUserId
    """)
    List<Long> findBlockedUserIdsByBlockerUserId(@Param("blockerUserId") Long blockerUserId);
}
