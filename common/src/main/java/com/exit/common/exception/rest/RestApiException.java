package com.exit.common.exception.rest;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;

import java.util.List;

@Getter
public class RestApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> errors;

    public RestApiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public RestApiException(ErrorCode errorCode, String detailMessage) {
        this(errorCode, detailMessage, null);
    }

    public RestApiException(ErrorCode errorCode, String detailMessage, List<String> errors) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.errors = errors;
    }
}