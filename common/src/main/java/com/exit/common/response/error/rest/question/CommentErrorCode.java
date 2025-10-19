package com.exit.common.response.error.rest.question;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    // 댓글 관련
    CREATE_COMMENT_FAIL("QUESTION_ERR_030", HttpStatus.BAD_REQUEST, "댓글 생성에 실패하였습니다."),
    DELETE_COMMENT_FAIL("QUESTION_ERR_031", HttpStatus.BAD_REQUEST, "댓글 삭제에 실패하였습니다."),
    GET_COMMENT_FAIL("QUESTION_ERR_032", HttpStatus.BAD_REQUEST, "댓글 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
