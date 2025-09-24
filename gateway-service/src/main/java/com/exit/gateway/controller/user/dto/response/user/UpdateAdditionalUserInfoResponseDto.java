package com.exit.gateway.controller.user.dto.response.user;

import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import lombok.Builder;

@Builder
public record UpdateAdditionalUserInfoResponseDto(
        Long userId,
        String nickName,
        String profile
) {
    public static UpdateAdditionalUserInfoResponseDto from(UpdateAdditionalUserInfoResponse response){
        return UpdateAdditionalUserInfoResponseDto.builder()
                .userId(response.getUserId())
                .nickName(response.getUserName())
                .profile(response.getUserProfile())
                .build();
    }
}
