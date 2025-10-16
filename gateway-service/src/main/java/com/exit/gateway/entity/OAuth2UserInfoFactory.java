package com.exit.gateway.entity;

import com.exit.gateway.controller.user.dto.response.auth.oauth2.KakaoOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.NaverOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> null;
        };
    }
}