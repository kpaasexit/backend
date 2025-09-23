package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.QuestionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionReportRepository extends JpaRepository<QuestionReport, Long> {
}