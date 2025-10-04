package com.exit.gateway.controller.user.dto.response.user;

import com.exit.common.grpc.UpdateDeviceResponse;
import lombok.Builder;

@Builder
public record UpdateDeviceResponseDto(
        String deviceId,
        String deviceType
) {
    public static UpdateDeviceResponseDto from(UpdateDeviceResponse response) {
        return UpdateDeviceResponseDto.builder()
                .deviceId(response.getDeviceId())
                .deviceType(response.getDeviceType())
                .build();
    }
}
