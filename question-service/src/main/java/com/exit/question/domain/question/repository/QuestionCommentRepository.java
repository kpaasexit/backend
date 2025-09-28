package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.QuestionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionCommentRepository extends JpaRepository<QuestionComment, Long> {
    boolean existsByIdAndWriterId(Long id, Long writerId);
}
