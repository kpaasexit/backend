package com.exit.user.controller.dto.request;

import com.exit.user.grpc.RefreshTokenRequest;
import lombok.Builder;

@Builder
public record RefreshTokenRequestDto(
        String refreshToken
) {
    public static RefreshTokenRequestDto from(RefreshTokenRequest request) {
        return RefreshTokenRequestDto.builder()
                .refreshToken(request.getRefreshToken())
                .build();
    }
}
