package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, Long> {

    @Query("SELECT qc FROM QuestionCategory qc WHERE " +
           "LOWER(qc.questionCategoryName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<QuestionCategory> findCategoriesByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT qc.* FROM question_categories qc " +
           "JOIN questions q ON q.question_category_id = qc.question_category_id " +
           "WHERE LOWER(q.question_title) LIKE LOWER(CONCAT('%', :title, '%')) " +
           "GROUP BY qc.question_category_id " +
           "ORDER BY COUNT(*) DESC " +
           "LIMIT 1", nativeQuery = true)
    QuestionCategory findMostUsedCategoryByTitlePattern(@Param("title") String title);
}