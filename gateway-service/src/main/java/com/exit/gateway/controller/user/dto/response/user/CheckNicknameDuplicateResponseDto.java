package com.exit.gateway.controller.user.dto.response.user;

import com.exit.common.grpc.CheckNicknameDuplicateResponse;
import lombok.Builder;

@Builder
public record CheckNicknameDuplicateResponseDto(
        Boolean isAvailable
) {
    public static CheckNicknameDuplicateResponseDto from(CheckNicknameDuplicateResponse response) {
        return CheckNicknameDuplicateResponseDto.builder()
                .isAvailable(response.getIsAvailable())
                .build();
    }
}
