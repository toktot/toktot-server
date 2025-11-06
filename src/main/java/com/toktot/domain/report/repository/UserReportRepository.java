package com.toktot.domain.report.repository;

import com.toktot.domain.report.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);

}
