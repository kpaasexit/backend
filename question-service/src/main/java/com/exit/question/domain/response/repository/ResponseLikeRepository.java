package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResponseLikeRepository extends JpaRepository<ResponseLike, Long> {
    Optional<ResponseLike> findByResponseIdAndUserId(Long responseId, Long userId);

    @Query("""
            select coalesce(count(*), 0)
            from ResponseLike rl
            where rl.responseId = :responseId
          """)
    Integer countByResponseId(Long responseId);
}