package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ResponseImageRepository extends JpaRepository<ResponseImage, Long> {
    @Query("select ri.responseImageUrl from ResponseImage ri where ri.responseId = :responseId")
    List<String> findAllByResponseId(Long responseId);

    List<ResponseImage> findAllByResponseIdIn(List<Long> responseIds);
}