package com.exit.question.domain.response.repository;

import com.exit.question.domain.question.QuestionComment;
import com.exit.question.domain.response.ResponseComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponseCommentRepository extends JpaRepository<ResponseComment, Long> {
    boolean existsByIdAndWriterId(Long commentId, Long userId);

    Page<ResponseComment> findAllByResponse_ResponseId(Long targetId, PageRequest pageRequest);
}
