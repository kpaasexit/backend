package com.exit.user.controller.dto.response;

public record LoginSuccessResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String profileImageUrl
) {
}
