package com.exit.common.response.error.rest;

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

    // 답변 관련
    CREATE_RESPONSE_FAIL("QUESTION_ERR_010", HttpStatus.BAD_REQUEST, "답변 등록에 실패하였습니다."),
    UPDATE_RESPONSE_FAIL("QUESTION_ERR_011", HttpStatus.BAD_REQUEST, "답변 업데이트에 실패하였습니다."),
    DELETE_RESPONSE_FAIL("QUESTION_ERR_012", HttpStatus.BAD_REQUEST, "답변 삭제에 실패하였습니다."),
    ADOPT_RESPONSE_FAIL("QUESTION_ERR_013", HttpStatus.BAD_REQUEST, "답변 채택에 실패하였습니다."),
    RECOMMEND_RESPONSE_FAIL("QUESTION_ERR_014", HttpStatus.BAD_REQUEST, "답변 추천에 실패하였습니다."),
    RESPONSE_REPORT_FAIL("QUESTION_ERR_015", HttpStatus.BAD_REQUEST, "답변 신고에 실패하였습니다."),

    // 추가 질문 관련
    CREATE_ADDITIONAL_QUESTION_FAIL("QUESTION_ERR_020", HttpStatus.BAD_REQUEST, "추가 질문 생성에 실패하였습니다."),
    GET_ADDITIONAL_QUESTION_LIST_FAIL("QUESTION_ERR_021", HttpStatus.BAD_REQUEST, "추가 질문 리스트 조회에 실패하였습니다."),

    // 댓글 관련
    CREATE_COMMENT_FAIL("QUESTION_ERR_030", HttpStatus.BAD_REQUEST, "댓글 생성에 실패하였습니다."),
    DELETE_COMMENT_FAIL("QUESTION_ERR_031", HttpStatus.BAD_REQUEST, "댓글 삭제에 실패하였습니다."),
    GET_COMMENT_FAIL("QUESTION_ERR_032", HttpStatus.BAD_REQUEST, "댓글 조회에 실패하였습니다."),

    // 기타
    GET_POPULAR_POST_FAIL("QUESTION_ERR_040", HttpStatus.BAD_REQUEST, "인기 게시글 조회에 실패하였습니다."),
    FILE_CONVERSION_FAIL("QUESTION_ERR_041", HttpStatus.BAD_REQUEST, "파일 변환 중 오류가 발생했습니다."),
    GET_MY_QUESTION_FAIL("QUESTION_ERR_042", HttpStatus.BAD_REQUEST, "내 질문 목록 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
