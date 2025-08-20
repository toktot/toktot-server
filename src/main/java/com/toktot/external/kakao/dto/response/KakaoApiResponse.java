package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoApiResponse<T>(
        @JsonProperty("documents")
        List<T> documents,

        @JsonProperty("meta")
        KakaoMeta meta
) {

    public boolean hasResults() {
        if (documents == null) {
            return false;
        }
        return !documents.isEmpty();
    }

    public int getResultCount() {
        if (documents == null) {
            return 0;
        }
        return documents.size();
    }

    public boolean hasMorePages() {
        if (meta == null) {
            return false;
        }
        return !meta.isEnd();
    }
}
