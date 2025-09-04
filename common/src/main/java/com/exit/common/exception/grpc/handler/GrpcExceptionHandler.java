package com.exit.common.exception.grpc.handler;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.exception.grpc.GrpcExceptionResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;

@GrpcAdvice
@RequiredArgsConstructor
@Order(value = Integer.MIN_VALUE)
public class GrpcExceptionHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(GrpcException.class)
    public Status businessExceptionHandler(final GrpcException ex) {

        GrpcExceptionResponseBody errorResponse = GrpcExceptionResponseBody.of(ex);

        try {
            return Status.INVALID_ARGUMENT
                    .withDescription(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            return Status.INTERNAL.withDescription("Serialization error");
        }
    }
}