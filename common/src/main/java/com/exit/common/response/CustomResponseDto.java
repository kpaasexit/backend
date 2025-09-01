package com.exit.common.response;

public record CustomResponseDto<T>(String code, String message, T result) {
}
