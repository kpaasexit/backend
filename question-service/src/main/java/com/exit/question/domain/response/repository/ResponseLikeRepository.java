package com.exit.question.domain.response.repository;

import com.exit.question.controller.dto.response.ResponseLikeCount;
import com.exit.question.domain.response.ResponseLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponseLikeRepository extends JpaRepository<ResponseLike, Long> {
    Optional<ResponseLike> findByResponseIdAndUserId(Long responseId, Long userId);

    @Query("""
              select count(*)
              from ResponseLike rl
              where rl.responseId = :responseId
            """)
    Integer countByResponseId(Long responseId);

    @Query("SELECT new com.exit.question.controller.dto.response.ResponseLikeCount(r.responseId, CAST(COUNT(r) AS integer)) " +
            "FROM ResponseLike r WHERE r.responseId IN :responseIds " +
            "GROUP BY r.responseId")
    List<ResponseLikeCount> countByResponseIdIn(List<Long> responseIds);
}