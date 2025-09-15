package com.exit.gateway.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String name;
    private String profileImageUrl;
    private String provider; // 소셜로그인시에만 설정됨
    private boolean isNewUser; // 신규 사용자 여부 (소셜로그인시에만 의미있음)
}