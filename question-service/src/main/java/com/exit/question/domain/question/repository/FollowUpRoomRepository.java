package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.FollowUpRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowUpRoomRepository extends JpaRepository<FollowUpRoom, Long> {
}
