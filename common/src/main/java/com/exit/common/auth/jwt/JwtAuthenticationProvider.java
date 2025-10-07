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
        Member member = jwtTokenProvider.getUserDetailFromToken(accessToken);
        return new JwtAuthenticationToken(member, accessToken);
    }
}
