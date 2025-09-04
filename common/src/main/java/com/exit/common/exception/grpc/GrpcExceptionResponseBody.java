package com.exit.common.exception.grpc;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@JsonPropertyOrder(value = {"errorCode"})
public class GrpcExceptionResponseBody {
    private final String errorCode;
    private final String developCode;
    private final String errorMessage;

    @Builder
    public GrpcExceptionResponseBody(String errorCode, String developCode, String errorMessage) {
        this.errorCode = errorCode;
        this.developCode = developCode;
        this.errorMessage = errorMessage;
    }

    public static GrpcExceptionResponseBody of(GrpcException ex) {
        return new GrpcExceptionResponseBody(ex.getGrpcErrorCode().getGrpcStatusCode().name(), ex.getGrpcErrorCode().getDevelopCode(), ex.getMessage());
    }
}