package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.QuestionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionImageRepository extends JpaRepository<QuestionImage, Long> {
    Optional<List<QuestionImage>> findAllByQuestionId(Long questionId);

    void deleteByQuestionId(Long questionId);
}