package com.exit.common.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
    private final JwtTokenProvider jwtTokenProvider;

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String accessToken = authentication.getCredentials().toString();
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        return new JwtAuthenticationToken(userId, accessToken);
    }
}
