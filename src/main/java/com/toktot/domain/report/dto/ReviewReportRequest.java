package com.toktot.domain.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toktot.domain.report.type.ReporterType;
import com.toktot.domain.report.type.ReviewReportType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.stream.Collectors;

public record ReviewReportRequest(
        @JsonProperty(value = "review_id")
        @NotNull(message = "신고할 리뷰 ID는 필수입니다.")
        Long reviewId,

        @JsonProperty(value = "reporter_type")
        @NotNull(message = "신고자 유형은 필수입니다.")
        ReporterType reporterType,

        @JsonProperty(value = "report_types")
        @NotEmpty(message = "신고 유형을 최소 1개 이상 선택해주세요.")
        @Size(max = 7, message = "신고 유형은 최대 7개까지 선택 가능합니다.")
        List<ReviewReportType> reportTypes,

        @JsonProperty(value = "other_reason")
        @NotEmpty(message = "상세 사유를 입력해 주세요.")
        @Size(max = 500, message = "상세 사유는 최대 500자까지 입력 가능합니다.")
        String otherReason,

        @JsonProperty(value = "privacy_consent")
        @NotNull(message = "개인정보 수집 동의는 필수입니다.")
        @AssertTrue(message = "개인정보 수집에 미동의할 경우 신고가 불가능합니다.")
        Boolean privacyConsent
) {
        public String getReportTypesAsJson() {
                if (reportTypes == null || reportTypes.isEmpty()) {
                        return "[]";
                }

                return reportTypes.stream()
                        .map(type -> "\"" + type.name() + "\"")
                        .collect(Collectors.joining(",", "[", "]"));
        }
}
