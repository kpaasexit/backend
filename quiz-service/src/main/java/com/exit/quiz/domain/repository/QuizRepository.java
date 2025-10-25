package com.exit.quiz.domain.repository;

import com.exit.quiz.controller.dto.response.CategoryQuizCountDto;
import com.exit.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query(value = "SELECT q.* FROM quizs q WHERE q.quiz_category_id = :categoryId " +
            "AND q.quiz_id NOT IN (SELECT qa.quiz_id FROM quiz_attempts qa WHERE qa.user_id = :userId) " +
            "ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Quiz> findRandomUnsolvedByCategoryIdAndUserId(@Param("categoryId") Short categoryId, @Param("userId") Long userId);

    @Query("""
                select new com.exit.quiz.controller.dto.response.CategoryQuizCountDto(
                    cast(q.quizCategory.id as long),
                    cast(count(distinct q.id) as int),
                    cast(count(distinct qa.quiz.id) as int)
                )
                from Quiz q left join QuizAttempts qa on qa.quiz.id = q.id and qa.userId = :userId
                group by q.quizCategory.id
            """)
    List<CategoryQuizCountDto> getQuizByCategoryId(@Param("userId") Long userId);

    @Query("SELECT q FROM Quiz q JOIN FETCH q.quizCategory WHERE q.id = :quizId")
    Optional<Quiz> findQuizWithQuizCategoryByQuizId(@Param("quizId") Long quizId);

    @EntityGraph(attributePaths = {"quizCategory"})
    Optional<Quiz> findQuizById(Long quizId);
}