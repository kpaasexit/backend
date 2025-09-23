package com.exit.question.domain.response.repository;

import com.exit.question.controller.dto.response.ResponseDetailDto;
import com.exit.question.domain.response.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
    Slice<Response> findAllByQuestionId(Long questionId, PageRequest pageRequest);
}