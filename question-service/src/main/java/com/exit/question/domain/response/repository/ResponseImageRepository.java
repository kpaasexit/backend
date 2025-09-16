package com.exit.question.domain.response.repository;

import com.exit.question.domain.response.ResponseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseImageRepository extends JpaRepository<ResponseImage, Long> {
}