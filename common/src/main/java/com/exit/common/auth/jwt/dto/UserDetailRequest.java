package com.exit.common.auth.jwt.dto;

public record UserDetailRequest(
        Long userId,
        String deviceId) {
}
