package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcCommentErrorCode implements GrpcErrorCode {
    NULL_COMMENT(Status.Code.NOT_FOUND, "COMMENT_ERR_001", "댓글이 존재하지 않음"),
    COMMENT_WRITER_MISMATCH(Status.Code.INVALID_ARGUMENT, "COMMENT_ERR_002", "댓글 작성자가 아닙니다"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
