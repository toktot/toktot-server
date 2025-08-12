package com.toktot.domain.report.repository;

import com.toktot.domain.report.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReporter_IdAndReview_Id(Long reporterId, Long reviewId);

}
