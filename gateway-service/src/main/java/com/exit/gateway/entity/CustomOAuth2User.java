package com.exit.gateway.entity;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
@Builder
public class CustomOAuth2User implements OAuth2User {
    private final Long userId;

    private final String nickname;
    private final String email;
    private final String registrationId;
    private final String accessToken;
    private final String refreshToken;

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(
                "userId", userId,
                "nickname", nickname,
                "email", email,
                "registrationId", registrationId
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}