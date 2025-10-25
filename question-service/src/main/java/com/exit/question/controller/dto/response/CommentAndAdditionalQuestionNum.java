package com.exit.question.controller.dto.response;

public record CommentAndAdditionalQuestionNum(
        Long targetId,
        Integer commentNum,
        Integer additionalQuestionNum
) {
}
