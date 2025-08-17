package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KakaoMeta {

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("pageable_count")
    private Integer pageableCount;

    @JsonProperty("is_end")
    private Boolean isEnd;

    @JsonProperty("same_name")
    private KakaoSameName sameName;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class KakaoSameName {

        @JsonProperty("region")
        private String[] region;

        @JsonProperty("keyword")
        private String keyword;

        @JsonProperty("selected_region")
        private String selectedRegion;
    }
}
