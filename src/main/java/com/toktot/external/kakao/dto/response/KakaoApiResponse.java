package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class KakaoApiResponse<T> {

    @JsonProperty("documents")
    private List<T> documents;

    @JsonProperty("meta")
    private KakaoMeta meta;

    public boolean hasResults() {
        return documents != null && !documents.isEmpty();
    }

    public int getResultCount() {
        return documents != null ? documents.size() : 0;
    }

    public boolean hasMorePages() {
        return meta != null && !meta.getIsEnd();
    }

    public int getTotalCount() {
        return meta != null ? meta.getTotalCount() : 0;
    }

}
