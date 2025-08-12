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

    public boolean canReportReview(Long userId, Long reviewId) {
        return !reviewReportRepository.existsByReporter_IdAndReview_Id(userId, reviewId);
    }

    @Transactional
    public void reportReview(ReviewReportRequest request, User reporter) {
        Review targetReview = findReview(request.reviewId());
        validateReviewReport(targetReview, reporter.getId());

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

    private void validateReviewReport(Review targetReview, Long reporterId) {
        if (targetReview.getUser().getId().equals(reporterId)) {
            throw new ToktotException(ErrorCode.CANNOT_REPORT_OWN_REVIEW);
        }

        if (!canReportReview(reporterId, targetReview.getId())) {
            throw new ToktotException(ErrorCode.DUPLICATE_REPORT);
        }
    }
}
