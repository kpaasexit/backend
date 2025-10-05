package com.exit.common.auth.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private Member member;
    private String token;

    public JwtAuthenticationToken(String token) {
        super(null);
        this.member = null;
        this.token = token;
        setAuthenticated(false);
    }

    public JwtAuthenticationToken(Member member, String token) {
        super(null);
        this.member = member;
        this.token = token;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.token;
    }

    @Override
    public Object getPrincipal() {
        return member;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        if (authenticated) {
            throw new IllegalArgumentException("옳지 않은 과정을 통해 인증되었습니다.");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.token = null;
    }
}
