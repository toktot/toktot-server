package com.toktot.domain.block;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.web.dto.block.UserBlockRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public void blockUser(UserBlockRequest request, User blocker) {
        User blockedUser = findBlockedUser(request.blockedUserId());

        checkSelfBlock(blocker.getId(), blockedUser.getId());
        checkDuplicateBlock(blocker.getId(), blockedUser.getId());

        UserBlock userBlock = UserBlock.create(blocker, blockedUser);
        userBlockRepository.save(userBlock);

        log.info("사용자 차단 완료 - blocker: {}, blocked: {}",
                blocker.getId(), blockedUser.getId());
    }

    private User findBlockedUser(Long blockedUserId) {
        return userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ToktotException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkSelfBlock(Long blockerUserId, Long blockedUserId) {
        if (blockerUserId.equals(blockedUserId)) {
            throw new ToktotException(ErrorCode.CANNOT_BLOCK_OWN_USER);
        }
    }

    private void checkDuplicateBlock(Long blockerUserId, Long blockedUserId) {
        if (userBlockRepository.existsByBlockerUser_IdAndBlockedUser_Id(blockerUserId, blockedUserId)) {
            throw new ToktotException(ErrorCode.DUPLICATE_USER_BLOCK);
        }
    }
}
