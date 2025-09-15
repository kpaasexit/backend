package com.exit.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken implements Serializable {
    private String jwtId;
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private Long expiresAt;
}