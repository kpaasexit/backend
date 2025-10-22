package com.exit.question.domain.question.repository;

import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.controller.dto.response.PopularPostDto;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.response.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("select new com.exit.question.controller.dto.request.NotificationContentDto(substring(q.questionContent, 1, 100), q.questionWriterId) " +
            "from Question q where q.questionId = :targetId")
    Optional<NotificationContentDto> findContentById(Long targetId);

    @Query(value = """
                    select new com.exit.question.controller.dto.response.PopularPostDto(
                            q.questionId,
                            qc.questionCategoryId,
                            q.questionWriterId,
                            q.questionTitle,
                            q.questionContent,
                            q.questionAnswerAdopt,
                            cast(count(r) as int),
                            q.createdAt
                    )
                    from Question q
                    join q.questionCategory qc
                    left join Response r on r.questionId = q.questionId
                    group by q.questionId, qc.questionCategoryId, q.questionWriterId,
                             q.questionTitle, q.questionContent, q.questionAnswerAdopt, q.createdAt
                    order by count(r) desc
            """)
    List<PopularPostDto> findTop5By();

    @Query("""
            select distinct new com.exit.question.controller.dto.response.QuestionListQueryResponseDto(
                q.questionId,
                qc.questionCategoryId,
                q.questionWriterId,
                q.questionTitle,
                q.questionContent,
                q.questionUrgency,
                q.questionAnswerType,
                q.questionAnswerAdopt,
                cast((select count(r2) from Response r2 where r2.questionId = q.questionId) as Long),
                q.createdAt
            )
            from Question q
            join q.questionCategory qc
            left join Response r on r.questionId = q.questionId
            where (qc.questionCategoryId in :categoryIds)
              and (:kw is null
                          or lower(q.questionTitle)  like lower(concat('%', :kw, '%'))
                          or lower(q.questionContent) like lower(concat('%', :kw, '%')))
            and (:isExist is null or :isExist = false or exists (
                    select 1 from Response r3 where r3.questionId = q.questionId
            ))
            """)
    Page<QuestionListQueryResponseDto> findQuestionsByFilter(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("kw") String keywordLike,
            @Param("isExist") Boolean isExist,
            Pageable pageable
    );

    @Query(value = """
            select new com.exit.question.controller.dto.response.PopularPostDto(
                q.questionId,
                qc.questionCategoryId,
                q.questionWriterId,
                q.questionTitle,
                q.questionContent,
                q.questionAnswerAdopt,
                cast(count(r) as int),
                q.createdAt
            )
            from Question q
            join q.questionCategory qc
            left join Response r on r.questionId = q.questionId
            where q.questionWriterId = :questionWriterId
            group by q.questionId, qc.questionCategoryId, q.questionWriterId,
                     q.questionTitle, q.questionContent, q.questionAnswerAdopt, q.createdAt
            """)
    Page<PopularPostDto> findByQuestionWriterId(Long questionWriterId, PageRequest pageRequest);
}