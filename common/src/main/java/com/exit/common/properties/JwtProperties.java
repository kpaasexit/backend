package com.exit.common.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class JwtProperties {

    @Value("${jwt.secret_key}")
    private String secret;

    @Value("${jwt.access_token.valid_time}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh_token.valid_time}")
    private Long refreshTokenExpiration;
}