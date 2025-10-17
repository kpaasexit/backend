package com.exit.common.response.error.rest.question;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionErrorCode implements ErrorCode {
    // 질문 관련
    CREATE_QUESTION_FAIL("QUESTION_ERR_001", HttpStatus.BAD_REQUEST, "질문 등록에 실패하였습니다."),
    GET_QUESTION_LIST_FAIL("QUESTION_ERR_002", HttpStatus.BAD_REQUEST, "질문 목록 조회에 실패하였습니다."),
    GET_QUESTION_DETAIL_FAIL("QUESTION_ERR_003", HttpStatus.BAD_REQUEST, "질문 상세 조회에 실패하였습니다."),
    QUESTION_REPORT_FAIL("QUESTION_ERR_004", HttpStatus.BAD_REQUEST, "질문 신고에 실패하였습니다."),
    CATEGORY_RECOMMEND_FAIL("QUESTION_ERR_005", HttpStatus.BAD_REQUEST, "카테고리 추천에 실패하였습니다."),
    SIMILAR_QUESTION_FAIL("QUESTION_ERR_006", HttpStatus.BAD_REQUEST, "유사 질문 조회에 실패하였습니다."),
    GET_MY_QUESTION_FAIL("QUESTION_ERR_042", HttpStatus.BAD_REQUEST, "내 질문 목록 조회에 실패하였습니다."),

    // 기타,
    GET_POPULAR_POST_FAIL("QUESTION_ERR_040", HttpStatus.BAD_REQUEST, "인기 게시글 조회에 실패하였습니다."),
    FILE_CONVERSION_FAIL("QUESTION_ERR_041", HttpStatus.BAD_REQUEST, "파일 변환 중 오류가 발생했습니다."),
    AVAILABLE_QUESTION_SERVER("QUESTION_ERR_099", HttpStatus.INTERNAL_SERVER_ERROR, "질문 서버 다운!!!!!"),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
