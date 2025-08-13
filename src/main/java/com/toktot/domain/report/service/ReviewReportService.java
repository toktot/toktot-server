package com.toktot.domain.report.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.report.ReviewReport;
import com.toktot.domain.report.repository.ReviewReportRepository;
import com.toktot.domain.review.Review;
import com.toktot.domain.review.repository.ReviewRepository;
import com.toktot.domain.user.User;
import com.toktot.web.dto.report.request.ReviewReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;

    public void canReportReview(Long userId, Long reviewId) {
        Review review = findReview(reviewId);
        checkSelfReport(userId, review.getUser().getId());
        checkDuplicateReport(userId, review.getId());
    }

    @Transactional
    public void reportReview(ReviewReportRequest request, User reporter) {
        Review targetReview = findReview(request.reviewId());
        checkSelfReport(reporter.getId(), targetReview.getUser().getId());
        checkDuplicateReport(reporter.getId(), targetReview.getId());

        ReviewReport reviewReport = ReviewReport.create(
                reporter,
                targetReview,
                request.getReportTypesAsJson(),
                request.reporterType(),
                request.otherReason()
        );
        targetReview.increaseReportCount();
        targetReview.getUser().increaseReportCount();

        reviewReportRepository.save(reviewReport);
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ToktotException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void checkSelfReport(Long reporterId, Long reportedUserId) {
        if (reporterId.equals(reportedUserId)) {
            throw new ToktotException(ErrorCode.CANNOT_REPORT_OWN_REVIEW);
        }
    }

    private void checkDuplicateReport(Long reporterId, Long reviewId) {
        if (reviewReportRepository.existsByReporter_IdAndReview_Id(reporterId, reviewId)) {
            throw new ToktotException(ErrorCode.DUPLICATE_REVIEW_REPORT);
        }
    }
}
