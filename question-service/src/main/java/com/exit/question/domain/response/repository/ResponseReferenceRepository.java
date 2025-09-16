package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseReferenceRepository extends JpaRepository<ResponseReference, Long> {
}