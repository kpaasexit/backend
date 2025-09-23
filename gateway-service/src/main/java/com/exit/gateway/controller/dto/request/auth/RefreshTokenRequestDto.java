package com.exit.gateway.controller.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequestDto {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}