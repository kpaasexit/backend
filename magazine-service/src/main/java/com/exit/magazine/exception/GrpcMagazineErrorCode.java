package com.exit.magazine.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcMagazineErrorCode implements GrpcErrorCode {
    MAGAZINE_NOT_FOUND(Status.Code.NOT_FOUND, "MAGAZINE_ERR_001", "매거진이 존재하지 않음")
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
