package com.exit.gateway.service.user.util;

import com.exit.common.grpc.UpdateDeviceRequest;
import com.exit.gateway.controller.user.dto.request.user.UpdateDeviceRequestDto;
import org.springframework.stereotype.Component;

@Component
public class UserGrpcMapper {
    public UpdateDeviceRequest getUpdateDeviceRequest(Long userId, UpdateDeviceRequestDto request) {
        return UpdateDeviceRequest.newBuilder()
                .setUserId(userId)
                .setDeviceId(request.deviceId())
                .setDeviceType(request.deviceType())
                .build();
    }
}
