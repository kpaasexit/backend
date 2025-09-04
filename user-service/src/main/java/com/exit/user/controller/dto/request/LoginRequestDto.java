package com.exit.user.controller.dto.request;

import com.exit.user.grpc.LoginRequest;
import com.exit.user.grpc.SignUpRequest;
import lombok.Builder;

@Builder
public record LoginRequestDto(
        String email,
        String password
) {
    public static LoginRequestDto from(LoginRequest request) {
        return LoginRequestDto.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }
}
