package com.exit.common.exception.grpc;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import lombok.Getter;

@Getter
public class GrpcException extends RuntimeException {
    private final GrpcErrorCode grpcErrorCode;

    public GrpcException(GrpcErrorCode grpcErrorCode, String message) {
        super(message);
        this.grpcErrorCode = grpcErrorCode;
    }

    public GrpcException(GrpcErrorCode grpcErrorCode) {
        this(grpcErrorCode, null);
    }
}