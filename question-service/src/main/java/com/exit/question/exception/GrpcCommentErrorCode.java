package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcCommentErrorCode implements GrpcErrorCode {
    // 댓글 관련
    NOT_FOUND_COMMENT(Status.Code.NOT_FOUND, "COMMENT_ERR_001", "댓글이 존재하지 않음"),
    COMMENT_WRITER_MISMATCH(Status.Code.INVALID_ARGUMENT, "COMMENT_ERR_002", "댓글 작성자가 아닙니다"),

    // 댓글 관련,
    UNAVAILABLE_COMMENT_TYPE(Status.Code.UNAVAILABLE, "COMMENT_ERR_005", "지원하지 않는 댓글 타입입니다."),

    // 댓글 작업 실패
    CREATE_COMMENT_FAILED(Status.Code.INTERNAL, "COMMENT_ERR_010", "댓글 생성에 실패했습니다."),
    DELETE_COMMENT_FAILED(Status.Code.INTERNAL, "COMMENT_ERR_011", "댓글 삭제에 실패했습니다."),
    GET_COMMENT_FAILED(Status.Code.INTERNAL, "COMMENT_ERR_012", "댓글 조회에 실패했습니다.")
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
