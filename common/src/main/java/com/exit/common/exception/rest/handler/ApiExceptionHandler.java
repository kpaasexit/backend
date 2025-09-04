package com.exit.common.exception.rest.handler;

import com.exit.common.exception.rest.ExceptionResponseBody;
import com.exit.common.exception.rest.RestApiException;
import com.exit.common.response.error.rest.ErrorCode;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

@GrpcAdvice
@Order(value = Integer.MIN_VALUE)
public class ApiExceptionHandler {
    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ExceptionResponseBody> businessExceptionHandler(final RestApiException apiException) {
        final ErrorCode errorCode = apiException.getErrorCode();
        final String details = apiException.getMessage();

        return createExceptionResponse(errorCode, ExceptionResponseBody.of(errorCode, details));
    }

    private ResponseEntity<ExceptionResponseBody> createExceptionResponse(ErrorCode errorCode, ExceptionResponseBody exceptionResponseBody) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(exceptionResponseBody);
    }
}