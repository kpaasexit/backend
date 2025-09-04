package com.exit.user.controller.dto.response;

import java.time.LocalDateTime;

public record LoginSuccessResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String email,
        String name,
        String phone,
        String profileImageUrl,
        LocalDateTime createdAt
) {
}
