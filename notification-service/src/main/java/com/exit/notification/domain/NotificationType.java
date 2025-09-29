package com.exit.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    QUESTION_ANSWER("질문에 답변이 달렸습니다"),
    QUESTION_COMMENT("질문에 댓글이 달렸습니다"),
    ANSWER_ADDITIONAL_QUESTION("답변에 추가 질문이 달렸습니다."),
    ANSWER_COMMENT("답변에 댓글이 달렸습니다");

    private final String description;
}