package com.toktot.web.controller.folder;

import com.toktot.domain.folder.service.FolderService;
import com.toktot.domain.user.User;
import com.toktot.web.dto.ApiResponse;
import com.toktot.web.dto.folder.request.FolderCreateRequest;
import com.toktot.web.dto.folder.response.FolderResponse;
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
                .setMessage("Review foloder read request received")
                .addKeyValue("userId", user.getId())
                .log();

        List<FolderResponse> response = folderService.ReadFolders(user);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
