package com.exit.common.response.error.rest.question;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseErrorCode implements ErrorCode {
    // 답변 관련
    CREATE_RESPONSE_FAIL("QUESTION_ERR_010", HttpStatus.BAD_REQUEST, "답변 등록에 실패하였습니다."),
    UPDATE_RESPONSE_FAIL("QUESTION_ERR_011", HttpStatus.BAD_REQUEST, "답변 업데이트에 실패하였습니다."),
    DELETE_RESPONSE_FAIL("QUESTION_ERR_012", HttpStatus.BAD_REQUEST, "답변 삭제에 실패하였습니다."),
    ADOPT_RESPONSE_FAIL("QUESTION_ERR_013", HttpStatus.BAD_REQUEST, "답변 채택에 실패하였습니다."),
    RECOMMEND_RESPONSE_FAIL("QUESTION_ERR_014", HttpStatus.BAD_REQUEST, "답변 추천에 실패하였습니다."),
    RESPONSE_REPORT_FAIL("QUESTION_ERR_015", HttpStatus.BAD_REQUEST, "답변 신고에 실패하였습니다."),
    GET_DETAIL_RESPONSE_FAIL("QUESTION_ERR_016", HttpStatus.BAD_REQUEST, "답변 목록 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
