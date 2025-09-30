package com.toktot.domain.folder.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record FolderUpdateRequest(
        @JsonProperty(value = "folder_name")
        @Size(max = 50, message = "폴더명은 최대 50자까지 입력 가능합니다.")
        String folderName
) {
}
