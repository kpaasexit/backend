package com.exit.magazine.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcMagazineErrorCode implements GrpcErrorCode {
    // 매거진 관련
    MAGAZINE_NOT_FOUND(Status.Code.NOT_FOUND, "MAGAZINE_ERR_001", "매거진이 존재하지 않음"),

    // 매거진 조회 실패
    GET_MAGAZINES_BY_CATEGORY_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_020", "카테고리별 매거진 조회 실패"),
    GET_MAGAZINE_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_021", "매거진 조회 실패"),
    GET_SCRAP_BOX_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_022", "스크랩 박스 조회 실패"),
    GET_RECOMMENDED_MAGAZINE_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_023", "추천 매거진 조회 실패"),

    // 매거진 스크랩 실패
    SCRAP_MAGAZINE_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_030", "매거진 스크랩 실패"),
    DELETE_SCRAP_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_031", "스크랩 삭제 실패"),
    SAVE_SCRAP_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_032", "스크랩 저장 실패"),

    // 사용자 정보 조회 실패
    GET_USER_INFO_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_040", "사용자 정보 조회 실패"),

    // 매거진 통합 검색 실패
    SEARCH_INTEGRATED_FAILED(Status.Code.INTERNAL, "MAGAZINE_ERR_060", "매거진 통합 검색 실패");

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
