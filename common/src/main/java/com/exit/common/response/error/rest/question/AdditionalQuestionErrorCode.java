package com.exit.common.response.error.rest.question;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdditionalQuestionErrorCode implements ErrorCode {
    // 추가 질문 관련
    CREATE_ADDITIONAL_QUESTION_FAIL("QUESTION_ERR_020", HttpStatus.BAD_REQUEST, "추가 질문 생성에 실패하였습니다."),
    GET_ADDITIONAL_QUESTION_LIST_FAIL("QUESTION_ERR_021", HttpStatus.BAD_REQUEST, "추가 질문 리스트 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
