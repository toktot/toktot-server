package com.toktot.domain.restaurant.controller;

import com.toktot.domain.restaurant.service.MenuSubmissionService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/restaurants")
@RequiredArgsConstructor
public class MenuSubmissionController {

    private final MenuSubmissionService menuSubmissionService;

    @PostMapping("/{restaurantId}/menu-images")
    public ResponseEntity<ApiResponse<Void>> uploadMenuImages(
            @PathVariable @Positive Long restaurantId,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal User user) {

        log.info("메뉴 이미지 업로드 요청 - userId: {}, restaurantId: {}, fileCount: {}",
                user.getId(), restaurantId, files != null ? files.length : 0);

        menuSubmissionService.saveMenuSubmissions(restaurantId, files, user);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드가 완료되었습니다."));
    }
}
