package com.toktot.domain.block;

import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/blocks/users")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @Valid @RequestBody UserBlockRequest request,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("사용자 차단")
                .addKeyValue("blockerUserId", user.getId())
                .addKeyValue("blockedUserId", request.blockedUserId())
                .log();

        userBlockService.blockUser(request, user);
        return ResponseEntity.ok(ApiResponse.success("사용자를 차단했습니다."));
    }
}
