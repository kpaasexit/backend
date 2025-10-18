package com.exit.common.exception.grpc.handler;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.exception.grpc.GrpcExceptionResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
@RequiredArgsConstructor
public class CustomGrpcExceptionHandler {
    private final ObjectMapper objectMapper;

    @GrpcExceptionHandler(GrpcException.class)
    public Status handleGrpcException(final GrpcException ex) {
        GrpcExceptionResponseBody errorResponse = GrpcExceptionResponseBody.of(ex);

        try {
            // 실제 GrpcException의 Status Code를 사용
            Status.Code statusCode = ex.getGrpcErrorCode().getGrpcStatusCode();
            String jsonDescription = objectMapper.writeValueAsString(errorResponse);
            return Status.fromCode(statusCode)
                    .withDescription(jsonDescription);
        } catch (Exception e) {
            log.error("Failed to serialize gRPC exception", e);
            return Status.INTERNAL.withDescription("Serialization error");
        }
    }
}