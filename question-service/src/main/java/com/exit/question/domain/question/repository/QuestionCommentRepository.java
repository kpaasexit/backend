package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.QuestionComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionCommentRepository extends JpaRepository<QuestionComment, Long> {
    boolean existsByIdAndWriterId(Long id, Long writerId);

    Page<QuestionComment> findAllByQuestion_QuestionId(Long targetId, PageRequest pageRequest);
}
