package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.HashTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HashTagRepository extends JpaRepository<HashTag, Long> {

    @Query("SELECT DISTINCT h.hashTagTitle, COUNT(h) as usage_count " +
           "FROM HashTag h " +
           "JOIN Question q ON h.questionId = q.questionId " +
           "WHERE LOWER(q.questionTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(q.questionContent) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "GROUP BY h.hashTagTitle " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> findPopularHashtagsByKeyword(@Param("keyword") String keyword);

    @Query("SELECT h.hashTagTitle, COUNT(h) as usage_count " +
           "FROM HashTag h " +
           "GROUP BY h.hashTagTitle " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> findMostPopularHashtags();

    @Query("SELECT DISTINCT h.hashTagTitle " +
           "FROM HashTag h " +
           "WHERE LOWER(h.hashTagTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<String> findHashtagsByPartialMatch(@Param("keyword") String keyword);
}