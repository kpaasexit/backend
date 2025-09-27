package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.FollowUpMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowUpMessageRepository extends JpaRepository<FollowUpMessage, Long> {
}
