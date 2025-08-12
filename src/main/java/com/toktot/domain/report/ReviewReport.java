package com.toktot.domain.report;

import com.toktot.domain.report.type.ReportStatus;
import com.toktot.domain.report.type.ReporterType;
import com.toktot.domain.review.Review;
import com.toktot.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "review_reports",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_review_reports_reporter_review",
            columnNames = {"reporter_user_id", "review_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false, length = 20)
    private ReporterType reporterType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "report_types", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reportTypes;

    @Column(name = "other_reason", length = 500)
    private String otherReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReviewReport create(User reporter, Review review, String reportTypes, ReporterType reporterType, String otherReason) {
        return ReviewReport.builder()
                .reporter(reporter)
                .review(review)
                .reportTypes(reportTypes)
                .reporterType(reporterType)
                .otherReason(otherReason)
                .build();
    }
}
