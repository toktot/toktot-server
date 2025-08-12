package com.toktot.web.dto.report.request;

import com.toktot.domain.report.type.ReporterType;
import com.toktot.domain.report.type.ReviewReportType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviewReportRequest(

        @NotNull(message = "신고할 리뷰 ID는 필수입니다.")
        Long reviewId,

        @NotNull(message = "신고자 유형은 필수입니다.")
        ReporterType reporterType,

        @NotEmpty(message = "신고 유형을 최소 1개 이상 선택해주세요.")
        @Size(max = 7, message = "신고 유형은 최대 7개까지 선택 가능합니다.")
        List<ReviewReportType> reportTypes,

        @Size(max = 500, message = "기타 사유는 최대 500자까지 입력 가능합니다.")
        String otherReason
) {
        public String getReportTypesAsJson() {
                return reportTypes.stream()
                        .map(Enum::name)
                        .reduce("[", (acc, type) -> acc.equals("[") ? acc + "\"" + type + "\"" : acc + ",\"" + type + "\"")
                        + "]";
        }
}
