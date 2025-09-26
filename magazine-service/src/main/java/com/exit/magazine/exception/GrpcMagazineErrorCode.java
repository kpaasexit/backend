package com.exit.magazine.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcMagazineErrorCode implements GrpcErrorCode {
    NULL_MAGAZINE(Status.Code.NOT_FOUND, "MAGAZINE_ERR_001")
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
