package com.exit.common.response.error.rest;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getDevelopCode();

    HttpStatus getHttpStatus();

    String getErrorDescription();
}
