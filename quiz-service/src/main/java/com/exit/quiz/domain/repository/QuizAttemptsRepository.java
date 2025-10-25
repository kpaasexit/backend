package com.exit.quiz.domain.repository;

import com.exit.quiz.controller.dto.response.GetSolvedQuiz;
import com.exit.quiz.domain.QuizAttempts;
import org.springframework.data.domain.Page;
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
                    select new com.exit.quiz.controller.dto.response.GetSolvedQuiz(
                        q.id,
                        substring(q.title, 1, 100)
                    )
                    from Quiz q
                    join q.quizCategory qc
                    where qc.id in(:categoryIdList)
                      and q.id in (
                          select distinct qa.quiz.id from QuizAttempts qa where qa.userId = :userId
                      )
            """)
    Page<GetSolvedQuiz> findByQuizCategoryIdIn(@Param("categoryIdList") List<Short> categoryIdList,
                                               @Param("userId") Long userId,
                                               Pageable pageable
    );
}