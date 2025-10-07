package com.exit.user.controller.dto.request;

import com.exit.common.grpc.RefreshTokenRequest;
import lombok.Builder;

@Builder
public record RefreshTokenRequestDto(
        String refreshToken,
        String deviceId
) {
    public static RefreshTokenRequestDto from(RefreshTokenRequest request) {
        return RefreshTokenRequestDto.builder()
                .refreshToken(request.getRefreshToken())
                .deviceId(request.getDeviceId())
                .build();
    }
}
