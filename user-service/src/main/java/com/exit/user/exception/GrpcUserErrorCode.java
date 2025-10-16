package com.exit.user.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcUserErrorCode implements GrpcErrorCode {
    // 사용자 관련
    USER_NOT_FOUND(Status.Code.NOT_FOUND, "USER_ERR_004", "사용자를 찾지 못함"),
    USER_ALREADY_DELETED(Status.Code.FAILED_PRECONDITION, "USER_ERR_016", "탈퇴한 회원입니다."),

    // 인프라 관련
    DB_CONNECTION_FAILED(Status.Code.UNAVAILABLE, "USER_ERR_010", "DB 연결 실패"),
    REDIS_CONNECTION_FAILED(Status.Code.UNAVAILABLE, "USER_ERR_011", "Redis DB 연결 실패"),

    // 사용자 정보 조회 실패
    GET_USER_NAME_FAILED(Status.Code.INTERNAL, "USER_ERR_017", "유저 닉네임 조회 실패"),
    GET_USER_NAMES_FAILED(Status.Code.INTERNAL, "USER_ERR_018", "다수 유저 닉네임 조회 실패"),
    GET_USER_NAME_AND_PROFILE_FAILED(Status.Code.INTERNAL, "USER_ERR_019", "유저 닉네임, 프로필 조회 실패"),
    GET_USERS_NAME_AND_PROFILE_FAILED(Status.Code.INTERNAL, "USER_ERR_020", "다수 유저 닉네임, 프로필 조회 실패"),
    GET_FCM_TOKEN_FAILED(Status.Code.INTERNAL, "USER_ERR_021", "유저 fcm 토큰 조회 실패"),

    // 사용자 정보 업데이트 실패
    UPDATE_DEVICE_FAILED(Status.Code.INVALID_ARGUMENT, "USER_ERR_012", "유저 fcm 업데이트 실패"),
    UPDATE_ADDITIONAL_INFO_FAILED(Status.Code.INTERNAL, "USER_ERR_022", "유저 추가 정보 업데이트 실패"),

    // 기타 작업 실패
    INCREASE_REPORT_COUNT_FAILED(Status.Code.INTERNAL, "USER_ERR_023", "유저 신고 횟수 증가 실패"),
    CHECK_NICKNAME_DUPLICATE_FAILED(Status.Code.INTERNAL, "USER_ERR_024", "닉네임 중복 확인 실패");

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}