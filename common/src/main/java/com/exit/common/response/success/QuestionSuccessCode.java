package com.exit.common.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionSuccessCode implements SuccessCode {
    QUESTION_CREATE_SUCCESS("QUESTION_OK_001", HttpStatus.CREATED, "질문 등록 성공"),
    QUESTION_LIST_SUCCESS("QUESTION_OK_002", HttpStatus.OK, "질문 목록 조회 성공"),
    QUESTION_DETAIL_SUCCESS("QUESTION_OK_003", HttpStatus.OK, "질문 상세 조회 성공"),
    ANSWER_CREATE_SUCCESS("QUESTION_OK_004", HttpStatus.CREATED, "답변 등록 성공"),
    ANSWER_ADOPT_SUCCESS("QUESTION_OK_005", HttpStatus.OK, "답변 채택 성공"),
    ANSWER_RECOMMEND_SUCCESS("QUESTION_OK_006", HttpStatus.OK, "답변 추천 성공"),
    QUESTION_REPORT_SUCCESS("QUESTION_OK_007", HttpStatus.CREATED, "질문 신고 성공"),
    ANSWER_REPORT_SUCCESS("QUESTION_OK_008", HttpStatus.CREATED, "답변 신고 성공"),
    CATEGORY_RECOMMEND_SUCCESS("QUESTION_OK_009", HttpStatus.OK, "카테고리 추천 성공"),
    SIMILAR_QUESTION_SUCCESS("QUESTION_OK_010", HttpStatus.OK, "유사 질문 조회 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
