package com.exit.gateway.controller.user.dto.request.user;

public record UpdateDeviceRequestDto(
        String deviceId,
        String deviceType
) {
}
