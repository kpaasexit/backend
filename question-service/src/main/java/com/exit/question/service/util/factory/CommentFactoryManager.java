package com.exit.question.service.util.factory;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.question.domain.CommentType;
import com.exit.question.exception.GrpcCommentErrorCode;
import com.exit.question.exception.GrpcQuestionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentFactoryManager {
    private final QuestionCommentFactory questionCommentFactory;
    private final ResponseCommentFactory responseCommentFactory;

    public CommentFactory getFactory(CommentType type) {
        return switch (type) {
            case QUESTION -> questionCommentFactory;
            case RESPONSE -> responseCommentFactory;
            default -> throw new GrpcException(GrpcCommentErrorCode.UNAVAILABLE_COMMENT_TYPE, "지원하지 않는 댓글 타입입니다.");
        };
    }
}
