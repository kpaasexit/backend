package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcResponseErrorCode implements GrpcErrorCode {
    // 답변 관련
    NOT_FOUND_RESPONSE(Status.Code.NOT_FOUND, "RESPONSE_ERR_002", "답변이 존재하지 않음"),
    ALREADY_RESPONSE_ADOPTED(Status.Code.INVALID_ARGUMENT, "RESPONSE_ERR_003", "이미 채택된 답변입니다"),

    // 답변 작업 실패
    CREATE_ANSWER_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_010", "답변 생성에 실패했습니다."),
    TOGGLE_ANSWER_LIKE_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_011", "답변 좋아요 처리에 실패했습니다."),
    UPDATE_RESPONSE_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_012", "답변 수정에 실패했습니다."),
    ANSWER_ADOPT_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_013", "답변 채택에 실패했습니다."),
    DELETE_RESPONSE_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_014", "답변 삭제에 실패했습니다."),
    ANSWER_REPORT_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_015", "답변 신고에 실패했습니다."),
    GET_DETAIL_RESPONSE_FAILED(Status.Code.INTERNAL, "RESPONSE_ERR_016", "답변 상세 조회에 실패했습니다."),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
