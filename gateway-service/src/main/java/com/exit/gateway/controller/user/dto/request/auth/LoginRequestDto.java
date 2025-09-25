package com.exit.gateway.controller.user.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "제공자는 필수입니다.")
    private String provider; // "google", "kakao", "naver"
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    private String authorizationCode;
    
    @NotBlank(message = "리다이렉트 URI는 필수입니다.")
    private String redirectUri;
}