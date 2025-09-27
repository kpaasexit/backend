package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.FollowUpImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowUpImageRepository extends JpaRepository<FollowUpImage, Long> {
}
