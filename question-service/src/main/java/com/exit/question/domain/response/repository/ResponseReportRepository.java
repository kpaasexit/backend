package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseReportRepository extends JpaRepository<ResponseReport, Long> {
}