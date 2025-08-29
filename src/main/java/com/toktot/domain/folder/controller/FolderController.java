package com.toktot.domain.folder.controller;

import com.toktot.domain.folder.service.FolderService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.domain.folder.dto.request.FolderCreateRequest;
import com.toktot.domain.folder.dto.request.FolderReviewCreateRequest;
import com.toktot.domain.folder.dto.response.FolderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
