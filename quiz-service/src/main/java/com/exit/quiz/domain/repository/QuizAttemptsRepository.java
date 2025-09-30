package com.exit.quiz.domain.repository;

import com.exit.quiz.controller.dto.response.GetSolvedQuiz;
import com.exit.quiz.domain.QuizAttempts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptsRepository extends JpaRepository<QuizAttempts, Long> {

    @Query("""
                    select distinct new com.exit.quiz.controller.dto.response.GetSolvedQuiz(q.id, substring(q.title, 1, 100) )
                    from QuizAttempts qa join qa.quiz q join q.quizCategory qc
                    where qc.id in(:categoryIdList) and qa.userId = :userId
            """)
    Slice<GetSolvedQuiz> findByQuizCategoryIdIn(@Param("categoryIdList") List<Long> categoryIdList,
                                                @Param("userId") Long userId,
                                                Pageable pageable
    );
}