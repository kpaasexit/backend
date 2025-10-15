package com.exit.common.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineSuccessCode implements SuccessCode {
    GET_MAGAZINE_LIST_SUCCESS("MAGAZINE_OK_001", HttpStatus.CREATED, "매거진 리스트 조회 성공"),
    GET_MAGAZINE_SUCCESS("MAGAZINE_OK_002", HttpStatus.OK, "매거진 상세 조회 성공"),
    SCRAP_MAGAZINE_SUCCESS("MAGAZINE_OK_003", HttpStatus.OK, "매거진 스크랩 성공"),
    GET_SCRAP_BOX_SUCCESS("MAGAZINE_OK_004", HttpStatus.OK, "매거진 스크랩 박스 조회 성공"),
    GET_RECOMMENDED_MAGAZINE_SUCCESS("MAGAZINE_OK_004", HttpStatus.OK, "추천 매거진 조회 성공"),
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
