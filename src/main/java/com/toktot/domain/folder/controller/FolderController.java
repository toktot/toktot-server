package com.toktot.domain.folder.controller;

import com.toktot.domain.folder.service.FolderService;
import com.toktot.domain.review.dto.response.search.ReviewListResponse;
import com.toktot.domain.review.service.ReviewSearchService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.folder.dto.request.FolderCreateRequest;
import com.toktot.domain.folder.dto.request.FolderUpdateRequest;
import com.toktot.domain.folder.dto.request.FolderReviewCreateRequest;
import com.toktot.domain.folder.dto.response.FolderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final ReviewSearchService reviewSearchService;

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @Valid @RequestBody FolderCreateRequest request,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("Review folder creation request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("folderName", request.folderName())
                .log();

        FolderResponse response = folderService.createFolder(user, request.folderName());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> readFolders(
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("Review folder read request received")
                .addKeyValue("userId", user.getId())
                .log();

        List<FolderResponse> response = folderService.readFolders(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<FolderResponse>>> readUserFolders(
            @PathVariable Long userId) {

        log.atInfo()
                .setMessage("특정 유저의 폴더 목록 조회 요청")
                .addKeyValue("targetUserId", userId)
                .log();

        List<FolderResponse> response = folderService.readUserFolders(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> updateFolderName(
            @PathVariable Long folderId,
            @Valid @RequestBody FolderUpdateRequest request,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("폴더명 변경 요청")
                .addKeyValue("userId", user.getId())
                .addKeyValue("folderId", folderId)
                .addKeyValue("newFolderName", request.folderName())
                .log();

        FolderResponse response = folderService.updateFolderName(user, folderId, request.folderName());

        return ResponseEntity.ok(ApiResponse.success("폴더명이 변경되었습니다.", response));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("폴더 삭제 요청")
                .addKeyValue("userId", user.getId())
                .addKeyValue("folderId", folderId)
                .log();

        folderService.deleteFolder(user, folderId);

        return ResponseEntity.ok(ApiResponse.success("폴더가 삭제되었습니다.", null));
    }

    @DeleteMapping("{folderId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteFolderReview(
            @PathVariable Long folderId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user) {

        log.atInfo()
                .setMessage("폴더에 저장된 리뷰 삭제 요청")
                .addKeyValue("userId", user.getId())
                .addKeyValue("folderId", folderId)
                .addKeyValue("reviewId", reviewId)
                .log();

        folderService.deleteFolderReview(user, folderId, reviewId);

        return ResponseEntity.ok(ApiResponse.success("저장된 리뷰가 삭제되었습니다.", null));
    }

    @PostMapping("/review-save")
    public ResponseEntity<ApiResponse<List<FolderResponse>>> createReviewToFolders(
            @AuthenticationPrincipal User user,
            @RequestBody FolderReviewCreateRequest request) {

        log.atInfo()
                .setMessage("Save review in folder creation request received")
                .addKeyValue("userId", user.getId())
                .addKeyValue("reviewId", request.reviewId())
                .addKeyValue("folders", request.folderIds())
                .log();

        folderService.createFolderReviews(user, request.folderIds(), request.reviewId());
        List<FolderResponse> response = folderService.readFolders(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reviews/{folderId}")
    public ResponseEntity<ApiResponse<Page<ReviewListResponse>>> getSavedReviews(
            @PathVariable Long folderId,
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.atInfo()
                .setMessage("저장한 리뷰 조회 요청")
                .addKeyValue("userId", user.getId())
                .addKeyValue("folderId", folderId)
                .log();

        Page<ReviewListResponse> response = reviewSearchService.getSavedReviews(
                user.getId(),
                folderId,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
