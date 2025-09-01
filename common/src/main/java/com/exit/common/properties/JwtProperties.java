package com.exit.common.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Getter
@Component
public class JwtProperties {
    @Value("${jwt.secret_key}")
    private String secretKey;

    @Value("${jwt.access_token.valid_time}")
    private Long accessTokenValidTime;

    @Value("${jwt.refresh_token.valid_time}")
    private Long refreshTokenValidTime;

    public byte[] getBytesSecretKey() {
        return secretKey.getBytes(StandardCharsets.UTF_8);
    }
}
