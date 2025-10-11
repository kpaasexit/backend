package com.exit.gateway.controller.user.dto.response.user;

import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import lombok.Builder;

@Builder
public record GetUserInfoResponseDto(
        String nickname,
        String profileUrl
) {
    public static GetUserInfoResponseDto from(UpdateAdditionalUserInfoResponse userInfo) {
        return GetUserInfoResponseDto.builder()
                .nickname(userInfo.getUserName())
                .profileUrl(userInfo.getUserProfile())
                .build();
    }
}
