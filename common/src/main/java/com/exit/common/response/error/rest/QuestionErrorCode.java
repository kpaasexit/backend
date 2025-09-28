package com.exit.common.response.error.rest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionErrorCode implements ErrorCode {
    CREATE_ADDITIONAL_QUESTION_FAIL("QUESTION_ERR_001", HttpStatus.BAD_REQUEST, "추가 질문 생성에 실패하였습니다."),
    GET_ADDITIONAL_QUESTION_LIST_FAIL("QUESTION_ERR_002", HttpStatus.BAD_REQUEST, "추가 질문 리스트 조회에 실패하였습니다."),
    CREATE_COMMENT_FAIL("QUESTION_ERR_003", HttpStatus.BAD_REQUEST, "댓글 생성에 실패하였습니다."),
    DELETE_COMMENT_FAIL("QUESTION_ERR_004", HttpStatus.BAD_REQUEST, "댓글 삭제에 실패하였습니다."),
    UPDATE_RESPONSE_FAIL("QUESTION_ERR_005", HttpStatus.BAD_REQUEST, "답변 업데이트에 실패하였습니다."),
    DELETE_RESPONSE_FAIL("QUESTION_ERR_006", HttpStatus.BAD_REQUEST, "답변 삭제에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
