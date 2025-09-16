package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseLikeRepository extends JpaRepository<ResponseLike, Long> {
}