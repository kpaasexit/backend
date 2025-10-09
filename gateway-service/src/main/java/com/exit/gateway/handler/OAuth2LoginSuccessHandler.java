package com.exit.gateway.handler;

import com.exit.gateway.controller.user.dto.response.auth.oauth2.KakaoOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.NaverOAuth2UserInfo;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.entity.CustomOAuth2User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        OAuth2UserInfo userInfo;
        if (oauth2User.getRegistrationId().equals("naver")) {
            userInfo = new NaverOAuth2UserInfo(oauth2User.getAttributes());
        } else {
            userInfo = new KakaoOAuth2UserInfo(oauth2User.getAttributes());
        }

        log.info("OAuth2 로그인 성공 - Provider: {}, User: {}", userInfo.getProvider(), userInfo.getName());

        try {
            // Device 관련 Cookie 삭제
            deleteCookie(response, "device_id");
            deleteCookie(response, "device_type");

            String returnTo = extractReturnToUrl(request);
            String origin = extractOrigin(request);

            // origin + returnTo를 조합하여 최종 리다이렉트 URL 생성
            String redirectUrl = origin + returnTo;

            // JWT 토큰과 함께 프론트엔드로 리다이렉트
            String finalRedirectUrl = String.format(
                    "%s?token=%s&refresh=%s",
                    redirectUrl,
                    URLEncoder.encode(oauth2User.getAccessToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(oauth2User.getRefreshToken(), StandardCharsets.UTF_8)
            );

            log.info("OAuth2 로그인 완료 - 리다이렉트: {}", finalRedirectUrl);
            response.sendRedirect(finalRedirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생", e);
            response.sendRedirect(redirectUrl + "?error=login_failed");
        }
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private String extractReturnToUrl(HttpServletRequest request) {
        String returnTo = request.getParameter("returnTo");

        if (returnTo == null || returnTo.isEmpty()) {
            returnTo = "/";
        }

        log.info("Extracted returnTo from query parameter: {}", returnTo);
        return returnTo;
    }

    private String extractOrigin(HttpServletRequest request) {
        String referer = request.getHeader("referer");
        String origin = "https://house-it.210-178-1-144.nip.io:5173"; // 기본값

        if (referer != null && !referer.isEmpty()) {
            try {
                java.net.URL url = new java.net.URL(referer);
                int port = url.getPort();
                if (port == -1) {
                    // 포트가 명시되지 않은 경우 (기본 포트 사용)
                    origin = url.getProtocol() + "://" + url.getHost();
                } else {
                    origin = url.getProtocol() + "://" + url.getHost() + ":" + port;
                }
            } catch (Exception e) {
                log.warn("Failed to parse referer header: {}", referer, e);
            }
        }

        log.info("Extracted origin from referer: {}", origin);
        return origin;
    }
}