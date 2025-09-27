package com.exit.question.domain.question.repository;

import com.exit.question.domain.question.FollowUpRoom;
import com.exit.question.domain.response.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowUpRoomRepository extends JpaRepository<FollowUpRoom, Long> {

    Optional<FollowUpRoom> findByResponse(Response response);
}
