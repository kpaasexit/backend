package com.exit.gateway.handler;

import com.exit.common.grpc.SocialLoginResponse;
import com.exit.gateway.entity.CustomOAuth2User;
import com.exit.gateway.controller.dto.response.oauth2.KakaoOAuth2UserInfo;
import com.exit.gateway.controller.dto.response.oauth2.NaverOAuth2UserInfo;
import com.exit.gateway.controller.dto.response.oauth2.OAuth2UserInfo;
import com.exit.gateway.service.UserGrpcClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.oauth2.redirect-url:http://localhost:3000/oauth2/redirect}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        OAuth2UserInfo userInfo;
        if(oauth2User.getRegistrationId().equals("naver")){
            userInfo = new NaverOAuth2UserInfo(oauth2User.getAttributes());
        } else {
            userInfo = new KakaoOAuth2UserInfo(oauth2User.getAttributes());
        }

        log.info("OAuth2 로그인 성공 - Provider: {}, User: {}", userInfo.getProvider(), userInfo.getName());

        try {
            // User-service에 gRPC로 소셜로그인 처리 요청
            // authorization code 대신 이미 획득한 사용자 정보를 전달

            // JWT 토큰과 함께 프론트엔드로 리다이렉트
            String finalRedirectUrl = String.format(
                "%s?token=%s&refresh=%s",
                redirectUrl,
                URLEncoder.encode(oauth2User.getAccessToken(), StandardCharsets.UTF_8),
                URLEncoder.encode(oauth2User.getRefreshToken(), StandardCharsets.UTF_8)
            );
            
            log.info("OAuth2 로그인 완료 - 리다이렉트: {}", redirectUrl);
            response.sendRedirect(finalRedirectUrl);
            
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생", e);
            response.sendRedirect(redirectUrl + "?error=login_failed");
        }
    }
}