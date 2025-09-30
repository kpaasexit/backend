package com.exit.quiz.domain.repository;

import com.exit.quiz.domain.QuizStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizStatsRepository extends JpaRepository<QuizStats, Long> {

    Optional<QuizStats> findByQuizId(Long quizId);
}