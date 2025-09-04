package com.exit.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    private Long userId;
    private String jti;
    private Long issuedAt;
    private Long expiresAt;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
}