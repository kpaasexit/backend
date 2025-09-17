package com.exit.question.domain.question.repository;

import com.exit.question.controller.dto.response.QuestionListQueryResponse;
import com.exit.question.domain.question.Question;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query(value = """
            select new com.exit.question.controller.dto.response.QuestionListQueryResponse(
                q.questionId,
                qc.questionCategoryId,
                q.questionWriterId,
                q.questionTitle,
                q.questionContent,
                q.questionUrgency,
                q.questionAnswerType,
                q.questionAnswerAdopt,
                (select count(r1) from Response r1 where r1.questionId = q.questionId),
                q.createdAt
            )
            from Question q
            join q.questionCategory qc
            where qc.questionCategoryId in :categoryIds
              and (
                    :keyword is null or :keyword = ''
                    or lower(q.questionTitle)  like lower(concat('%', :keyword, '%'))
                    or lower(q.questionContent) like lower(concat('%', :keyword, '%'))
                  )
            order by q.createdAt desc
            """)
    Slice<QuestionListQueryResponse> findQuestionsByFilter(@Param("categoryIds") List<Short> categoryIds, @Param("keyword") String keyword, Pageable pageable);
}