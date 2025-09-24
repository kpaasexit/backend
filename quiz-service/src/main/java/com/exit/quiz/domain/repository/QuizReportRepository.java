package com.exit.quiz.domain.repository;

import com.exit.quiz.domain.QuizReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizReportRepository extends JpaRepository<QuizReport, Long> {
}