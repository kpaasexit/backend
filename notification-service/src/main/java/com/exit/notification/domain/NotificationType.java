package com.exit.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    NEW_ANSWER_ON_QUESTION("작성하신 질문에 새로운 답변이 등록되었습니다."),
    NEW_COMMENT("작성하신 글에 새로운 댓글이 달렸습니다."),
    NEW_ADDITIONAL_QUESTION_ON_ANSWER("작성하신 답변에 추가 질문이 등록되었습니다."),
    NEW_ANSWER_ON_ADDITIONAL_QUESTION("작성하신 추가 질문에 답변이 등록되었습니다."),
    ANSWER_ADOPTED("작성하신 답변이 채택되었습니다."),
    ;

    private final String title;
}