package com.toktot.web.dto.folder.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toktot.domain.folder.Folder;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FolderResponse(
        Long folderId,
        String folderName,
        Long reviewCount,
        LocalDateTime createdAt
) {

    public static FolderResponse from(Folder folder) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getFolderName())
                .build();
    }

    public static FolderResponse from(Folder folder, Long reviewCount) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getFolderName())
                .reviewCount(reviewCount != null ? reviewCount : 0L)
                .build();
    }

}
