package com.toktot.domain.report.service;

import com.toktot.common.exception.ErrorCode;
import com.toktot.common.exception.ToktotException;
import com.toktot.domain.report.UserReport;
import com.toktot.domain.report.repository.UserReportRepository;
import com.toktot.domain.user.User;
import com.toktot.domain.user.repository.UserRepository;
import com.toktot.web.dto.report.request.UserReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReportService {

    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;

    public void canReportUser(Long reporterUserId, Long reportedUserId) {
        checkSelfReport(reporterUserId, reportedUserId);
        checkReportedUser(reportedUserId);
        checkDuplicateReport(reporterUserId, reportedUserId);
    }

    @Transactional
    public void reportUser(UserReportRequest request, User reporter) {
        canReportUser(reporter.getId(), request.reportedUserId());
        User reportedUser = findReportedUser(request.reportedUserId());

        UserReport userReport = UserReport.create(
                reporter,
                reportedUser,
                request.getReportTypesAsJson(),
                request.otherReason()
        );
        reportedUser.increaseReportCount();
        userReportRepository.save(userReport);
    }

    private User findReportedUser(Long reportedUserId) {
        return userRepository.findById(reportedUserId)
                .orElseThrow(() -> new ToktotException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkSelfReport(Long reporterUserId, Long reportedUserId) {
        if (reporterUserId.equals(reportedUserId)) {
            throw new ToktotException(ErrorCode.CANNOT_REPORT_OWN_USER);
        }
    }

    private void checkDuplicateReport(Long reporterUserId, Long reportedUserId) {
        if (userReportRepository.existsByReporterIdAndReportedUserId(reporterUserId, reportedUserId)) {
            throw new ToktotException(ErrorCode.DUPLICATE_USER_REPORT);
        }
    }

    private void checkReportedUser(Long reportedUserId) {
        if (!userRepository.existsById(reportedUserId)) {
            throw new ToktotException(ErrorCode.USER_NOT_FOUND);
        }
    }

}
