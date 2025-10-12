package com.exit.question.domain.response.repository;

import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.domain.response.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
    Slice<Response> findAllByQuestionId(Long questionId, PageRequest pageRequest);

    // 채택된 답변 조회
    Optional<Response> findByQuestionIdAndResponseAdoptTrue(Long questionId);

    // 채택되지 않은 답변 조회 (생성 순으로 정렬)
    Slice<Response> findAllByQuestionIdAndResponseAdoptFalse(Long questionId, PageRequest pageRequest);

    @Query("select new com.exit.question.controller.dto.request.NotificationContentDto(" +
            "substring(r.responseContent, 0, 100) as content, r.responseWriterId) " +
            "from Response r where r.questionId = :targetId")
    Optional<NotificationContentDto> findContentById(Long targetId);
}