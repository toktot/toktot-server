package com.toktot.external.kakao.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoMeta(
        @JsonProperty("total_count")
        Integer totalCount,

        @JsonProperty("pageable_count")
        Integer pageableCount,

        @JsonProperty("is_end")
        Boolean isEnd
) {}
