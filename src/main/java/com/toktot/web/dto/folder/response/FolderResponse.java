package com.toktot.web.dto.folder.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.folder.Folder;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FolderResponse(
        @JsonProperty(value = "folder_id")
        Long folderId,

        @JsonProperty(value = "folder_name")
        String folderName,

        @JsonProperty(value = "review_count")
        Long reviewCount,

        @JsonProperty(value = "created_at")
        LocalDateTime createdAt,

        @JsonProperty(value = "is_default")
        Boolean isDefault
) {

    public static FolderResponse fromNewFolder(Folder folder) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getFolderName())
                .reviewCount(0L)
                .createdAt(folder.getCreatedAt())
                .isDefault(folder.getIsDefault())
                .build();
    }

    public static FolderResponse from(Folder folder, Long reviewCount) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getFolderName())
                .reviewCount(reviewCount != null ? reviewCount : 0L)
                .createdAt(folder.getCreatedAt())
                .isDefault(folder.getIsDefault())
                .build();
    }

}
