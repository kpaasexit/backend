package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseCommentRepository extends JpaRepository<ResponseComment, Long> {
    boolean existsByIdAndWriterId(Long commentId, Long userId);
}
