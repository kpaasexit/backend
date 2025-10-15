package com.exit.common.response.error.rest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineErrorCode implements ErrorCode {
    NULL_MAGAZINE("MAGAZINE_ERR_001", HttpStatus.BAD_REQUEST, "매거진이 존재하지 않습니다."),
    GET_MAGAZINE_LIST_FAIL("MAGAZINE_ERR_002", HttpStatus.BAD_REQUEST, "매거진 리스트 조회에 실패하였습니다."),
    SCRAP_MAGAZINE_FAIL("MAGAZINE_ERR_003", HttpStatus.BAD_REQUEST, "매거진 스크랩을 실패하였습니다."),
    GET_SCRAPBOX_FAIL("MAGAZINE_ERR_004", HttpStatus.BAD_REQUEST, "매거진 스크랩 보관함 조회에 실패하였습니다."),
    GET_RECOMMENDED_MAGAZINE_FAIL("MAGAZINE_ERR_005", HttpStatus.BAD_REQUEST, "추천 매거진 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
