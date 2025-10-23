package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import io.grpc.Status.Code;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuestionErrorCode implements GrpcErrorCode {
    // 질문 관련
    NOT_FOUND_QUESTION(Status.Code.NOT_FOUND, "QUESTION_ERR_002", "질문을 찾을 수 없습니다."),
    UNAVAILABLE_QUESTION_CATEGORY(Status.Code.NOT_FOUND, "QUESTION_ERR_006", "해당 카테고리가 존재하지 않습니다."),
    EXIST_ADOPTED_RESPONSE(Status.Code.ALREADY_EXISTS, "QUESTION_ERR_003", "이미 채택된 답변이 있습니다."),

    // 질문 작업 실패
    CREATE_QUESTION_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_010", "질문 생성에 실패했습니다."),
    QUESTION_REPORT_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_011", "질문 신고에 실패했습니다."),
    GET_QUESTION_LIST_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_012", "질문 목록 조회에 실패했습니다."),
    GET_QUESTION_DETAIL_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_013", "질문 상세 조회에 실패했습니다."),
    GET_POPULAR_POST_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_014", "인기 게시글 조회에 실패했습니다."),
    GET_MY_QUESTION_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_015", "내 질문 조회에 실패했습니다."),
    CATEGORY_RECOMMEND_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_016", "카테고리 추천에 실패했습니다."),
    SIMILAR_QUESTION_FAILED(Status.Code.INTERNAL, "QUESTION_ERR_017", "유사 질문 조회에 실패했습니다."),
    ALREADY_EXISTS_RESPONSE(Status.Code.ALREADY_EXISTS, "QUESTION_ERR_018", "이미 답변이 달려있어, 수정이 불가능합니다."),
    MODIFY_QUESTION_FAILED(Status.Code.ALREADY_EXISTS, "QUESTION_ERR_019", "질문 수정에 실패하였습니다."),

    // 질문 이미지 작업 실패
    NOT_EXIST_QUESTION_IMAGE(Status.Code.NOT_FOUND, "QUESTION_ERR_019", "질문 이미지가 없습니다."),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}