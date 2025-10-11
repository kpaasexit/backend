package com.exit.question.domain.question.repository;

import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.controller.dto.response.PopularPostDto;
import com.exit.question.controller.dto.response.QuestionListQueryResponseDto;
import com.exit.question.domain.question.Question;
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
    @Query(value = """
            select new com.exit.question.controller.dto.response.QuestionListQueryResponseDto(
                q.questionId,
                qc.questionCategoryId,
                q.questionWriterId,
                q.questionTitle,
                q.questionContent,
                q.questionUrgency,
                q.questionAnswerType,
                q.questionAnswerAdopt,
                cast((select count(r1) from Response r1 where r1.questionId = q.questionId) as int),
                q.createdAt
            )
            from Question q
            join q.questionCategory qc
            where (qc.questionCategoryId in :categoryIds)
              and (
                    :keyword is null or :keyword = ''
                    or lower(q.questionTitle)  like lower(concat('%', :keyword, '%'))
                    or lower(q.questionContent) like lower(concat('%', :keyword, '%'))
                  )
            order by q.createdAt desc
            """)
    Slice<QuestionListQueryResponseDto> findQuestionsByFilter(@Param("categoryIds") List<Long> categoryIds, @Param("keyword") String keyword, Pageable pageable);

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
                cast((select count(r1) from Response r1 where r1.questionId = q.questionId) as int),
                q.createdAt
            )
            from Question q
            join q.questionCategory qc
            order by (select count(r1) from Response r1 where r1.questionId = q.questionId) desc
            limit 5
            """)
    List<PopularPostDto> findTop5ByResponseCount();

}