package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcAdditionalQuestionErrorCode implements GrpcErrorCode {
    // 추가 질문 관련
    NULL_FOLLOW_UP_ROOM(Status.Code.NOT_FOUND, "ADDITIONAL_QUESTION_ERR_001", "추가 질문방이 존재하지 않음"),
    NULL_FOLLOW_UP_MESSAGE(Status.Code.NOT_FOUND, "ADDITIONAL_QUESTION_ERR_002", "추가 질문 메시지가 존재하지 않음"),
    WAIT_OPPONENT_MESSAGE(Status.Code.NOT_FOUND, "ADDITIONAL_QUESTION_ERR_003", "상대방의 응답을 기다려주세요"),

    // 추가 질문 작업 실패
    CREATE_ADDITIONAL_QUESTION_MESSAGE_FAILED(Status.Code.INTERNAL, "ADDITIONAL_QUESTION_ERR_010", "추가 질문 메시지 생성에 실패했습니다."),
    GET_ADDITIONAL_QUESTION_FAILED(Status.Code.INTERNAL, "ADDITIONAL_QUESTION_ERR_011", "추가 질문 조회에 실패했습니다."),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
