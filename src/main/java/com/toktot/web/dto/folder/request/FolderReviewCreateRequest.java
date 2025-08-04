package com.toktot.web.dto.folder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FolderReviewCreateRequest(
        @JsonProperty(value = "folder_ids")
        @NotEmpty(message = "저장할 폴더를 최소 1개 이상 선택해주세요.")
        List<Long> folderIds
) {
}
