package com.exit.question.domain;

import java.time.LocalDateTime;

public interface Comment {
    Long getCommentId();

    Long getAuthorId();

    String getContent();

    LocalDateTime getCreatedAt();
}
