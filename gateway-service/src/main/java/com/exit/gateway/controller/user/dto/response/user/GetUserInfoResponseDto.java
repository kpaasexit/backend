package com.exit.gateway.controller.user.dto.response.user;

import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import lombok.Builder;

@Builder
public record GetUserInfoResponseDto(
        String nickname,
        String profileUrl
) {
    public static GetUserInfoResponseDto from(UpdateAdditionalUserInfoResponse userInfo) {

        GetUserInfoResponseDtoBuilder builder = GetUserInfoResponseDto.builder();
        if(!userInfo.getUserProfile().isEmpty()) {
            builder.profileUrl(userInfo.getUserProfile());
        }

        return builder
                .nickname(userInfo.getUserName())
                .build();
    }
}
