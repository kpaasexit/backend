package com.exit.question.domain.response.repository;

import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.controller.dto.response.AiBestResponseDto;
import com.exit.question.controller.dto.response.CommentAndAdditionalQuestionNum;
import com.exit.question.domain.response.Response;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
    Page<Response> findAllByQuestionId(Long questionId, PageRequest pageRequest);

    // 채택된 답변 조회
    Optional<Response> findByQuestionIdAndResponseAdoptTrue(Long questionId);

    // 채택되지 않은 답변 조회 (생성 순으로 정렬)
    Slice<Response> findAllByQuestionIdAndResponseAdoptFalse(Long questionId, PageRequest pageRequest);

    @Query("select new com.exit.question.controller.dto.request.NotificationContentDto(" +
            "substring(r.responseContent, 0, 100) as content, r.responseWriterId) " +
            "from Response r where r.responseId = :targetId")
    Optional<NotificationContentDto> findContentById(Long targetId);

    @Query(
            """
                    select new com.exit.question.controller.dto.response.AiBestResponseDto(
                                        q.questionTitle, r.responseContent, q.questionId, r.responseId)
                                                            from Response r
                                        inner join Question q on q.questionId = r.questionId
                                                            left join ResponseLike rl on rl.responseId = r.responseId
                                                            where r.responseWriterId = 1
                                        group by q.questionTitle, r.responseContent, q.questionId, r.responseId
                                        order by count(rl) desc
                                        limit 5
                    """
    )
    List<AiBestResponseDto> findAiBestResponseTop5();

    @Query(
            """
                        SELECT new com.exit.question.controller.dto.response.CommentAndAdditionalQuestionNum(
                                r.responseId,
                                CAST(COUNT(DISTINCT rc.id) AS int),
                                CAST(COUNT(DISTINCT fum.followUpMessageId) AS int)
                            )
                            FROM Response r
                            LEFT JOIN ResponseComment rc ON rc.response.responseId = r.responseId
                            LEFT JOIN FollowUpRoom fur ON fur.response.responseId = r.responseId
                            LEFT JOIN FollowUpMessage fum ON fum.followUpRoom.followUpRoomId = fur.followUpRoomId
                            WHERE r.responseId in :responseIds
                            GROUP BY r.responseId
                    """
    )
    List<CommentAndAdditionalQuestionNum> findCommentAndAdditionalQuestionNumByResponseId(@Param("responseIds") List<Long> responseIds);

    boolean existsByQuestionIdAndResponseWriterId(Long questionId, Long responseWriterId);
}